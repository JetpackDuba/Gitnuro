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
    object CredentialsRequested : CredentialsState()
    object CredentialsDenied : CredentialsState()
    data class CredentialsAccepted(val user: String, val password: String) : CredentialsState()
}