package com.jetpackduba.gitnuro.credentials

import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

private const val PASSWORD_FIELD_IDENTIFIER = "Passphrase"
private val passphrasesMap = mutableMapOf<String, String>()

class GpgCredentialsProvider @Inject constructor(
    private val credentialsStateManager: CredentialsStateManager,
) : CredentialsProvider() {

    var isRetry: Boolean = false
    private var credentialsSet: Pair<String, String>? = null

    override fun isInteractive(): Boolean = true

    override fun supports(vararg items: CredentialItem?): Boolean {
        println(items)
        return true
    }

    override fun get(uri: URIish?, vararg items: CredentialItem?): Boolean {
        val item = items.firstOrNull {
            it?.promptText == PASSWORD_FIELD_IDENTIFIER
        }

        val informationalMessage = items.firstOrNull {
            it is CredentialItem.InformationalMessage
        }

        val promptText = informationalMessage?.promptText

        if (item != null && item is CredentialItem.Password) {

            // Check if the passphrase was store in-memory before
            if (promptText != null && promptText.contains("fingerprint")) {
                val savedPassphrase = passphrasesMap[promptText]

                if (savedPassphrase != null) {
                    item.value = savedPassphrase.toCharArray()

                    return true
                }
            }

            // Request passphrase
            val credentials = runBlocking {
                credentialsStateManager.requestGpgCredentials(
                    isRetry = isRetry,
                    // Use previously set credentials for cases where this method is invoked again (like when the passphrase is not correct)
                    password = credentialsSet?.second ?: ""
                )
            }


            item.value = credentials.password.toCharArray()

            if (promptText != null)
                credentialsSet = promptText to credentials.password

            return true
        }

        return false
    }

    fun savePasswordInMemory() {
        credentialsSet?.let { credentials ->
            passphrasesMap[credentials.first] = credentials.second
        }
    }
}