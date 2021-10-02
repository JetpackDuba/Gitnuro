package credentials

class CredentialsStateManager {

}

sealed class CredentialsState {
    object CredentialsRequested: CredentialsState()
    object CredentialsDenied: CredentialsState()
    data class CredentialsAccepted(val user: String, val password: String): CredentialsState()
}