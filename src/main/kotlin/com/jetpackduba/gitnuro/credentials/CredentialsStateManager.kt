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

    fun requestCredentials(credentialsRequest: CredentialsRequest) {
        updateState(credentialsRequest)
    }
}

sealed interface CredentialsState {
    data object None : CredentialsState
    data object CredentialsDenied : CredentialsState
}

sealed interface CredentialsAccepted : CredentialsState {
    data class SshCredentialsAccepted(val password: String) : CredentialsAccepted
    data class GpgCredentialsAccepted(val password: String) : CredentialsAccepted
    data class HttpCredentialsAccepted(val user: String, val password: String) : CredentialsAccepted
    data class LfsCredentialsAccepted(val user: String, val password: String) : CredentialsAccepted
}

sealed interface CredentialsRequest : CredentialsState {
    data object SshCredentialsRequest : CredentialsRequest
    data class GpgCredentialsRequest(val isRetry: Boolean, val password: String) : CredentialsRequest
    data object HttpCredentialsRequest : CredentialsRequest
    data object LfsCredentialsRequest : CredentialsRequest
}

