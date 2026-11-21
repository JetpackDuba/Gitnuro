package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class Identity(val name: String?, val email: String?) {
    override fun toString(): String {
        return "$name <$email>"
    }

    fun formatted(showEmail: Boolean): String {
        return if (showEmail) {
            this.toString()
        } else {
            this.name.orEmpty()
        }
    }
}

fun emptyIdentity() = Identity(name = null, email = null)