package app.credentials

import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish

class HttpCredentialsProvider : CredentialsProvider() {
    private val credentialsStateManager = CredentialsStateManager

    override fun isInteractive(): Boolean {
        return true
    }

    override fun supports(vararg items: CredentialItem?): Boolean {
        println(items)
        val fields = items.map { credentialItem -> credentialItem?.promptText }
        return if (fields.isEmpty()) {
            true
        } else
            fields.size == 2 &&
                    fields.contains("Username") &&
                    fields.contains("Password")
    }

    override fun get(uri: URIish?, vararg items: CredentialItem?): Boolean {
        credentialsStateManager.updateState(CredentialsState.CredentialsRequested)

        @Suppress("ControlFlowWithEmptyBody")
        var credentials = credentialsStateManager.currentCredentialsState
        while (credentials is CredentialsState.CredentialsRequested) {
            credentials = credentialsStateManager.currentCredentialsState
        }

        if(credentials is CredentialsState.CredentialsAccepted) {
            val userItem = items.firstOrNull { it?.promptText == "Username" }
            val passwordItem = items.firstOrNull { it?.promptText == "Password" }

            if(userItem is CredentialItem.Username &&
                passwordItem is CredentialItem.Password) {

                userItem.value = credentials.user
                passwordItem.value = credentials.password.toCharArray()

                return true
            }
        }

        return false
    }

}