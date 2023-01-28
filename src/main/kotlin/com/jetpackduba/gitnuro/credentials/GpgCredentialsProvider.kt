package com.jetpackduba.gitnuro.credentials

import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

private const val PASSWORD_FIELD_IDENTIFIER = "Passphrase"

class GpgCredentialsProvider @Inject constructor(
    private val credentialsStateManager: CredentialsStateManager,
) : CredentialsProvider() {
    override fun isInteractive(): Boolean = true

    override fun supports(vararg items: CredentialItem?): Boolean {
        println(items)
        return true
    }

    override fun get(uri: URIish?, vararg items: CredentialItem?): Boolean {
        val item = items.firstOrNull {
            it?.promptText == PASSWORD_FIELD_IDENTIFIER
        }

        if (item != null && item is CredentialItem.Password) {
            credentialsStateManager.updateState(CredentialsState.GpgCredentialsRequested)

            var credentials = credentialsStateManager.currentCredentialsState

            while (credentials is CredentialsState.GpgCredentialsRequested) {
                credentials = credentialsStateManager.currentCredentialsState
            }

            if (credentials is CredentialsState.GpgCredentialsAccepted) {
                item.value = credentials.password.toCharArray()
                return true
            }
        }

        return false
    }

}