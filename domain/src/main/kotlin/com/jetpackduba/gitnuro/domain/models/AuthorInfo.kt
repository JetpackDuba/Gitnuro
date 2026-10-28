package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class AuthorInfo(
    val globalIdentity: Identity,
    val repositoryIdentity: Identity,
) {
    fun identityToUse() = Identity(
        repositoryIdentity.name ?: globalIdentity.name,
        repositoryIdentity.email ?: globalIdentity.email,
    )
}


