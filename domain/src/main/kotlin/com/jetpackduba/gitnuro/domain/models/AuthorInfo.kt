package com.jetpackduba.gitnuro.domain.models

// TODO Refactor to global and local Identity instead of 4 fields
data class AuthorInfo(
    val globalName: String?,
    val globalEmail: String?,
    val name: String?,
    val email: String?,
) {
    fun toIdentity() = Identity(
        name ?: globalName ?: "",
        email ?: globalEmail ?: "",
    )
}

