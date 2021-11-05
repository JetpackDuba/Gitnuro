package app.credentials

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

//TODO Mark as a singleton when dagger is implemented
object CredentialsStateManager {
    private val _credentialsState = MutableStateFlow<CredentialsState>(CredentialsState.None)
    val credentialsState: StateFlow<CredentialsState>
        get() = _credentialsState

    val currentCredentialsState: CredentialsState
        get() = credentialsState.value

    fun updateState(newCredentialsState: CredentialsState) {
        _credentialsState.value = newCredentialsState
    }
}

sealed class CredentialsState {
    object None : CredentialsState()
    sealed class CredentialsRequested : CredentialsState()
    object SshCredentialsRequested : CredentialsRequested()
    object HttpCredentialsRequested : CredentialsRequested()
    object CredentialsDenied : CredentialsState()
    sealed class CredentialsAccepted : CredentialsState()
    data class SshCredentialsAccepted(val password: String) : CredentialsAccepted()
    data class HttpCredentialsAccepted(val user: String, val password: String) : CredentialsAccepted()
}