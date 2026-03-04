package com.jetpackduba.gitnuro.domain.models

sealed interface CredentialsType {
    data class SshCredentials(
        val url: String,
        val password: String,
    ) : CredentialsType

    data class HttpCredentials(
        val url: String,
        val user: String,
        val password: String,
        val isLfs: Boolean,
    ) : CredentialsType
}