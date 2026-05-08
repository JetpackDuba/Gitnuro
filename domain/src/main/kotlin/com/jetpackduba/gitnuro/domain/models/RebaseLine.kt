package com.jetpackduba.gitnuro.domain.models

import org.eclipse.jgit.lib.AbbreviatedObjectId

data class RebaseLine(
    val action: RebaseAction,
    val commit: String,
    val shortMessage: String,
)

enum class RebaseAction {
    PICK,
    REWORD,
    SQUASH,
    FIXUP,
    EDIT,
    DROP,
    COMMENT;
}