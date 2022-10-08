package com.jetpackduba.gitnuro.credentials

import com.jetpackduba.gitnuro.exceptions.NotSupportedHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import java.io.*
import java.util.concurrent.TimeUnit

private const val TIMEOUT_MIN = 1L

class HttpCredentialsProvider @AssistedInject constructor(
    private val credentialsStateManager: CredentialsStateManager,
    @Assisted val git: Git?,
) : CredentialsProvider() {
    override fun isInteractive(): Boolean {
        return true
    }

    override fun supports(vararg items: CredentialItem?): Boolean {
        val fields = items.map { credentialItem -> credentialItem?.promptText }
        return if (fields.isEmpty()) {
            true
        } else
            fields.size == 2 &&
                    fields.contains("Username") &&
                    fields.contains("Password")
    }

    override fun get(uri: URIish, vararg items: CredentialItem): Boolean {
        val userItem = items.firstOrNull { it.promptText == "Username" }
        val passwordItem = items.firstOrNull { it.promptText == "Password" }

        if (userItem !is CredentialItem.Username || passwordItem !is CredentialItem.Password) {
            return false
        }

        val externalCredentialsHelper = getExternalCredentialsHelper(uri, git)

        if (externalCredentialsHelper == null) {
            val credentials = askForCredentials()

            if (credentials is CredentialsState.HttpCredentialsAccepted) {
                userItem.value = credentials.user
                passwordItem.value = credentials.password.toCharArray()

                return true
            }

            return false

        } else {
            when (handleExternalCredentialHelper(externalCredentialsHelper, uri, items)) {
                ExternalCredentialsRequestResult.SUCCESS -> return true
                ExternalCredentialsRequestResult.FAIL -> return false
                ExternalCredentialsRequestResult.CREDENTIALS_NOT_STORED -> {
                    val credentials = askForCredentials()
                    if (credentials is CredentialsState.HttpCredentialsAccepted) {
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
        credentials: CredentialsState.HttpCredentialsAccepted
    ) {
        val process = Runtime.getRuntime()
            .exec(String.format("${externalCredentialsHelper.path} %s", "store"));

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
        credentialsStateManager.updateState(CredentialsState.HttpCredentialsRequested)
        var credentials = credentialsStateManager.currentCredentialsState
        while (credentials is CredentialsState.CredentialsRequested) {
            credentials = credentialsStateManager.currentCredentialsState
        }

        return credentials
    }

    private fun handleExternalCredentialHelper(
        externalCredentialsHelper: ExternalCredentialsHelper,
        uri: URIish,
        items: Array<out CredentialItem>
    ): ExternalCredentialsRequestResult {
        val process = Runtime.getRuntime()
            .exec(String.format("${externalCredentialsHelper.path} %s", "get"))

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
        if(process.isAlive) {
            process.destroy()
            return ExternalCredentialsRequestResult.FAIL
        }

        bufferedReader.use {
            var line: String;
            while (bufferedReader.readLine().also { line = it } != null && !(usernameSet && passwordSet)) {
                if (line.startsWith("username=")) {
                    val split = line.split("=")
                    val userName = split.getOrNull(1) ?: return ExternalCredentialsRequestResult.CREDENTIALS_NOT_STORED

                    val userNameItem = items.firstOrNull { it.promptText == "Username" }

                    if (userNameItem is CredentialItem.Username) {
                        userNameItem.value = userName
                        usernameSet = true
                    }

                } else if (line.startsWith("password=")) {
                    val split = line.split("=")
                    val password = split.getOrNull(1) ?: return ExternalCredentialsRequestResult.CREDENTIALS_NOT_STORED

                    val passwordItem = items.firstOrNull { it.promptText == "Password" }

                    if (passwordItem is CredentialItem.Password) {
                        passwordItem.value = password.toCharArray()
                        passwordSet = true
                    }
                }
            }
        }

        return if (usernameSet && passwordSet)
            ExternalCredentialsRequestResult.SUCCESS
        else
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

        if(credentialHelperPath == "cache" || credentialHelperPath == "store") {
            throw NotSupportedHelper("Invalid credentials: \"$credentialHelperPath\" is not yet supported")
        }

        // Use getString instead of getBoolean as boolean has a default value by we want null if the config field is not set
        val uriSpecificUseHttpHelper = config.getString("credential", hostWithProtocol, "useHttpPath")
        val genericUseHttpHelper = config.getBoolean("credential", "useHttpPath", false)

        val useHttpPath = uriSpecificUseHttpHelper?.toBoolean() ?: genericUseHttpHelper

        return ExternalCredentialsHelper(credentialHelperPath, useHttpPath)
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