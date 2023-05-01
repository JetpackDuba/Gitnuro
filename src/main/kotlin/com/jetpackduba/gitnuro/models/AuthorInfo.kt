package com.jetpackduba.gitnuro.models

import org.eclipse.jgit.lib.PersonIdent

data class AuthorInfo(
    val globalName: String?,
    val globalEmail: String?,
    val name: String?,
    val email: String?,
) {
    fun toPersonIdent() = PersonIdent(
        name ?: globalName ?: "",
        email ?: globalEmail ?: "",
    )
}

