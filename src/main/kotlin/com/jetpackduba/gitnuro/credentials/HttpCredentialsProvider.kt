package com.jetpackduba.gitnuro.credentials

import com.jetpackduba.gitnuro.exceptions.NotSupportedHelper
import com.jetpackduba.gitnuro.git.remote_operations.CredentialsCache
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.managers.IShellManager
import com.jetpackduba.gitnuro.preferences.AppSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialItem.*
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

private const val TIMEOUT_MIN = 1L
private const val TAG = "HttpCredentialsProvider"

class HttpCredentialsProvider @AssistedInject constructor(
    private val credentialsStateManager: CredentialsStateManager,
    private val shellManager: IShellManager,
    private val appSettings: AppSettings,
    private val credentialsCacheRepository: CredentialsCacheRepository,
    @Assisted val git: Git?,
) : CredentialsProvider(), CredentialsCache {

    private var credentialsCached: CredentialsType.HttpCredentials? = null

    override fun isInteractive(): Boolean {
        return true
    }

    override fun supports(vararg items: CredentialItem?): Boolean {
        val fields = items.map { credentialItem -> credentialItem?.promptText }
        val isEmpty = fields.isEmpty()

        val isUserPasswordAuth = fields.size == 2 &&
                fields.contains("Username") &&
                fields.contains("Password")

        val isAskingForSslDisable = items.any { it is YesNoType }

        return isEmpty || isUserPasswordAuth || isAskingForSslDisable
    }

    override fun get(uri: URIish, vararg items: CredentialItem): Boolean {
        val itemsMap = items.map { "${it::class.simpleName} - ${it.promptText}" }

        printLog(TAG, "Items are $itemsMap")

        val sslTrustNowItem = items
            .filterIsInstance<YesNoType>()
            .firstOrNull { it.promptText.contains(JGitText.get().sslTrustNow) }

        val userItem = items
            .filterIsInstance<Username>()
            .firstOrNull()

        val passwordItem = items
            .filterIsInstance<Password>()
            .firstOrNull()

        if (userItem == null || passwordItem == null) {
            return false
        }

        if (sslTrustNowItem != null) {
            sslTrustNowItem.value = appSettings.verifySsl
        }

        val externalCredentialsHelper = getExternalCredentialsHelper(uri, git)

        if (externalCredentialsHelper == null) {
            val cachedCredentials = credentialsCacheRepository.getCachedHttpCredentials(uri.toString())

            if (cachedCredentials == null) {
                val credentials = askForCredentials()

                if (credentials is CredentialsAccepted.HttpCredentialsAccepted) {
                    userItem.value = credentials.user
                    passwordItem.value = credentials.password.toCharArray()

                    if (appSettings.cacheCredentialsInMemory) {
                        credentialsCached = CredentialsType.HttpCredentials(
                            url = uri.toString(),
                            userName = credentials.user,
                            password = credentials.password,
                        )
                    }

                    return true
                } else if (credentials is CredentialsState.CredentialsDenied) {
                    throw CancellationException("Credentials denied")
                }
            } else {
                userItem.value = cachedCredentials.userName
                passwordItem.value = cachedCredentials.password.toCharArray()

                return true
            }

            return false

        } else {
            when (handleExternalCredentialHelper(externalCredentialsHelper, uri, items)) {
                ExternalCredentialsRequestResult.SUCCESS -> return true
                ExternalCredentialsRequestResult.FAIL -> return false
                ExternalCredentialsRequestResult.CREDENTIALS_NOT_STORED -> {
                    val credentials = askForCredentials()
                    if (credentials is CredentialsAccepted.HttpCredentialsAccepted) {
                        userItem.value = credentials.user
                        passwordItem.value = credentials.password.toCharArray()

                        saveCredentialsInExternalHelper(uri, externalCredentialsHelper, credentials)

                        return true
                    }

                    return false
                }
            }
        }
    }

    private fun saveCredentialsInExternalHelper(
        uri: URIish,
        externalCredentialsHelper: ExternalCredentialsHelper,
        credentials: CredentialsAccepted.HttpCredentialsAccepted
    ) {
        val process = shellManager.runCommandProcess(listOf(externalCredentialsHelper.path, "store"))

        val output = process.outputStream // write to the input stream of the helper
        val bufferedWriter = BufferedWriter(OutputStreamWriter(output))

        bufferedWriter.use {
            bufferedWriter.write("protocol=${uri.scheme}\n")
            bufferedWriter.write("host=${uri.host}\n")

            if (externalCredentialsHelper.useHttpPath) {
                bufferedWriter.write("path=${uri.path}\n")
            }

            bufferedWriter.write("username=${credentials.user}\n")
            bufferedWriter.write("password=${credentials.password}\n")
            bufferedWriter.write("")

            bufferedWriter.flush()
        }
    }

    private fun askForCredentials(): CredentialsState {
        credentialsStateManager.updateState(CredentialsRequested.HttpCredentialsRequested)
        var credentials = credentialsStateManager.currentCredentialsState
        while (credentials is CredentialsRequested) {
            credentials = credentialsStateManager.currentCredentialsState
        }

        return credentials
    }

    private fun handleExternalCredentialHelper(
        externalCredentialsHelper: ExternalCredentialsHelper,
        uri: URIish,
        items: Array<out CredentialItem>
    ): ExternalCredentialsRequestResult {
        val process = shellManager.runCommandProcess(listOf(externalCredentialsHelper.path, "get"))

        val output = process.outputStream // write to the input stream of the helper
        val input = process.inputStream // reads from the output stream of the helper

        val bufferedWriter = BufferedWriter(OutputStreamWriter(output))
        val bufferedReader = BufferedReader(InputStreamReader(input))

        bufferedWriter.use {
            bufferedWriter.write("protocol=${uri.scheme}\n")
            bufferedWriter.write("host=${uri.host}\n")

            if (externalCredentialsHelper.useHttpPath) {
                bufferedWriter.write("path=${uri.path}\n")
            }

            bufferedWriter.write("")

            bufferedWriter.flush()
        }

        var usernameSet = false
        var passwordSet = false

        process.waitFor(TIMEOUT_MIN, TimeUnit.MINUTES)

        // If the process is alive after $TIMEOUT_MIN, it means that it hasn't given an answer and then finished
        if (process.isAlive) {
            process.destroy()
            return ExternalCredentialsRequestResult.FAIL
        }

        bufferedReader.use {
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null && !(usernameSet && passwordSet)) {
                val safeLine = line ?: continue

                if (safeLine.startsWith("username=")) {
                    val split = safeLine.split("=")
                    val userName = split.getOrNull(1) ?: return ExternalCredentialsRequestResult.CREDENTIALS_NOT_STORED

                    val userNameItem = items.firstOrNull { it is CredentialItem.Username }

                    if (userNameItem is CredentialItem.Username) {
                        userNameItem.value = userName
                        usernameSet = true
                    }

                } else if (safeLine.startsWith("password=")) {
                    val split = safeLine.split("=")
                    val password = split.getOrNull(1) ?: return ExternalCredentialsRequestResult.CREDENTIALS_NOT_STORED

                    val passwordItem = items.firstOrNull { it is CredentialItem.Password }

                    if (passwordItem is CredentialItem.Password) {
                        passwordItem.value = password.toCharArray()
                        passwordSet = true
                    }
                }
            }
        }

        return if (usernameSet && passwordSet) {
            ExternalCredentialsRequestResult.SUCCESS
        } else
            ExternalCredentialsRequestResult.CREDENTIALS_NOT_STORED
    }

    private fun getExternalCredentialsHelper(uri: URIish, git: Git?): ExternalCredentialsHelper? {
        val config = if (git == null) {
            val homePath = System.getProperty("user.home")
            val configFile = File("$homePath/.gitconfig")

            Config().apply {
                if (configFile.exists()) {
                    fromText(configFile.readText())
                }
            }
        } else {
            git.repository.config
        }

        val hostWithProtocol = "${uri.scheme}://${uri.host}"

        val genericCredentialHelper = config.getString("credential", null, "helper")
        val uriSpecificCredentialHelper = config.getString("credential", hostWithProtocol, "helper")
        val credentialHelperPath = uriSpecificCredentialHelper ?: genericCredentialHelper ?: return null

        if (credentialHelperPath == "cache" || credentialHelperPath == "store") {
            throw NotSupportedHelper("Invalid credentials helper: \"$credentialHelperPath\" is not yet supported")
        }

        // TODO Try to use "git-credential-manager-core" when "manager-core" is detected. Works for linux but requires testing for mac/windows
        if (credentialHelperPath == "manager-core") {
            throw NotSupportedHelper("Invalid credentials helper \"$credentialHelperPath\". Please specify the full path of Git Credential Manager in your .gitconfig")
        }

        // Use getString instead of getBoolean as boolean has a default value by we want null if the config field is not set
        val uriSpecificUseHttpHelper = config.getString("credential", hostWithProtocol, "useHttpPath")
        val genericUseHttpHelper = config.getBoolean("credential", "useHttpPath", false)

        val useHttpPath = uriSpecificUseHttpHelper?.toBoolean() ?: genericUseHttpHelper

        return ExternalCredentialsHelper(credentialHelperPath, useHttpPath)
    }

    override suspend fun cacheCredentialsIfNeeded() {
        credentialsCached?.let {
            credentialsCacheRepository.cacheHttpCredentials(it)
        }
    }
}

data class ExternalCredentialsHelper(
    val path: String,
    val useHttpPath: Boolean,
)

enum class ExternalCredentialsRequestResult {
    SUCCESS,
    FAIL,
    CREDENTIALS_NOT_STORED;
}