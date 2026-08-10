package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.models.RebaseLine

private const val TAG = "RebaseInteractiveViewMo"

enum class RebaseAction(val displayName: String, val value: RebaseLine.Action) {
    PICK("Pick", RebaseLine.Action.PICK),
    REWORD("Reword", RebaseLine.Action.REWORD),
    SQUASH("Squash", RebaseLine.Action.SQUASH),
    FIXUP("Fixup", RebaseLine.Action.FIXUP),
    EDIT("Edit", RebaseLine.Action.EDIT),
    DROP("Drop", RebaseLine.Action.DROP),
    COMMENT("Comment", RebaseLine.Action.COMMENT);
}

fun RebaseLine.Action.toRebaseAction(): RebaseAction {
    return when (this) {
        RebaseLine.Action.PICK -> RebaseAction.PICK
        RebaseLine.Action.REWORD -> RebaseAction.REWORD
        RebaseLine.Action.EDIT -> RebaseAction.EDIT
        RebaseLine.Action.SQUASH -> RebaseAction.SQUASH
        RebaseLine.Action.FIXUP -> RebaseAction.FIXUP
        RebaseLine.Action.COMMENT -> RebaseAction.COMMENT
        RebaseLine.Action.DROP -> RebaseAction.DROP
    }
}