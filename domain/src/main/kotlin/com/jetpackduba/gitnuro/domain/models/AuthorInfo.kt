package com.jetpackduba.gitnuro.domain.models

import org.eclipse.jgit.lib.PersonIdent


// TODO Refactor to global and local Identity instead of 4 fields
data class AuthorInfo(
    val globalName: String?,
    val globalEmail: String?,
    val name: String?,
    val email: String?,
)
// TODO remove this when refactor is finished. This mapping should be done in the data layer
 {
    fun toPersonIdent() = PersonIdent(
        name ?: globalName ?: "",
        email ?: globalEmail ?: "",
    )
}

