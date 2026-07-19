package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class RebaseLine(
    val action: Action,
    val commit: String,
    val shortMessage: String,
    val fullMessage: String,
    val modifiedMessage: String? = null,
) {
    enum class Action {
        PICK,
        REWORD,
        SQUASH,
        FIXUP,
        EDIT,
        DROP,
        COMMENT;
    }
}

