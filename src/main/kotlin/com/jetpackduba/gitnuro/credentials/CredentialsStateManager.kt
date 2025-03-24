package com.jetpackduba.gitnuro.credentials

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

// TODO Being a singleton, we may have problems if multiple tabs request credentials at the same time
@Singleton
class CredentialsStateManager @Inject constructor() {
    private val mutex = Mutex()
    private val _credentialsState = MutableStateFlow<CredentialsState>(CredentialsState.None)
    val credentialsState: StateFlow<CredentialsState>
        get() = _credentialsState

    suspend fun requestHttpCredentials(): CredentialsAccepted.HttpCredentialsAccepted {
        return requestAwaitingCredentials(CredentialsRequest.HttpCredentialsRequest)
    }

    suspend fun requestSshCredentials(): CredentialsAccepted.SshCredentialsAccepted {
        return requestAwaitingCredentials(CredentialsRequest.SshCredentialsRequest)
    }

    suspend fun requestGpgCredentials(isRetry: Boolean, password: String): CredentialsAccepted.GpgCredentialsAccepted {
        return requestAwaitingCredentials(CredentialsRequest.GpgCredentialsRequest(isRetry, password))
    }

    suspend fun requestLfsCredentials(): CredentialsAccepted.LfsCredentialsAccepted {
        return requestAwaitingCredentials(CredentialsRequest.LfsCredentialsRequest)
    }

    fun credentialsDenied() {
        _credentialsState.value = CredentialsState.CredentialsDenied
    }

    fun httpCredentialsAccepted(user: String, password: String) {
        _credentialsState.value = CredentialsAccepted.HttpCredentialsAccepted(user, password)
    }

    fun sshCredentialsAccepted(password: String) {
        _credentialsState.value = CredentialsAccepted.SshCredentialsAccepted(password)
    }

    fun gpgCredentialsAccepted(password: String) {
        _credentialsState.value = CredentialsAccepted.GpgCredentialsAccepted(password)
    }

    fun lfsCredentialsAccepted(user: String, password: String) {
        _credentialsState.value = CredentialsAccepted.LfsCredentialsAccepted(user, password)
    }

    private suspend inline fun <reified T : CredentialsAccepted> requestAwaitingCredentials(credentialsRequest: CredentialsRequest): T {
        mutex.withLock {
            assert(this.credentialsState.value is CredentialsState.None)

            _credentialsState.value = credentialsRequest

            val credentialsResult = this.credentialsState
                .first { it !is CredentialsRequest }

            _credentialsState.value = CredentialsState.None

            return when (credentialsResult) {
                is T -> credentialsResult
                is CredentialsState.CredentialsDenied -> throw CancellationException("Credentials denied")
                else -> throw IllegalStateException("Unexpected credentials result")
            }
        }
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
    data class LfsCredentialsAccepted(val user: String, val password: String) : CredentialsAccepted {
        companion object {
            fun fromCachedCredentials(credentials: CredentialsCacheType.HttpCredentialsCache): LfsCredentialsAccepted {
                return LfsCredentialsAccepted(credentials.user, credentials.password)
            }
        }
    }
}

sealed interface CredentialsRequest : CredentialsState {
    data object SshCredentialsRequest : CredentialsRequest
    data class GpgCredentialsRequest(val isRetry: Boolean, val password: String) : CredentialsRequest
    data object HttpCredentialsRequest : CredentialsRequest
    data object LfsCredentialsRequest : CredentialsRequest
}

