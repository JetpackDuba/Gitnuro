package com.jetpackduba.gitnuro.credentials

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

// TODO Being a singleton, we may have problems if multiple tabs request credentials at the same time
@Singleton
class CredentialsStateManager @Inject constructor() {
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