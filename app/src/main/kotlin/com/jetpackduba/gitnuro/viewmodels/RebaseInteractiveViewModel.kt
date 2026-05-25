package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.exceptions.InvalidMessageException
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.AbortRebaseUseCase
import com.jetpackduba.gitnuro.domain.usecases.GetCommitFromRebaseLineUseCase
import com.jetpackduba.gitnuro.domain.usecases.GetRepositoryStateUseCase
import com.jetpackduba.gitnuro.domain.usecases.ResumeRebaseInteractiveUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RebaseTodoLine.Action
import javax.inject.Inject

private const val TAG = "RebaseInteractiveViewMo"

sealed interface RebaseInteractiveViewState {
    object Loading : RebaseInteractiveViewState
    data class Loaded(val stepsList: List<RebaseLine>, val messages: Map<String, String>) : RebaseInteractiveViewState
    data class Failed(val error: String) : RebaseInteractiveViewState
}

data class RebaseLine(
    val rebaseAction: RebaseAction,
    val commit: AbbreviatedObjectId,
    val shortMessage: String,
) {
    fun toRebaseTodoLine(): RebaseTodoLine {
        return RebaseTodoLine(
            rebaseAction.toAction(),
            commit,
            shortMessage
        )
    }
}

enum class RebaseAction(val displayName: String) {
    PICK("Pick"),
    REWORD("Reword"),
    SQUASH("Squash"),
    FIXUP("Fixup"),
    EDIT("Edit"),
    DROP("Drop"),
    COMMENT("Comment");

    fun toAction(): Action {
        return when (this) {
            PICK -> Action.PICK
            REWORD -> Action.REWORD
            SQUASH -> Action.SQUASH
            FIXUP -> Action.FIXUP
            EDIT -> Action.EDIT
            COMMENT -> Action.COMMENT
            DROP -> throw NotImplementedError("To action should not be called when the RebaseAction is DROP")
        }
    }
}

fun Action.toRebaseAction(): RebaseAction {
    return when (this) {
        Action.PICK -> RebaseAction.PICK
        Action.REWORD -> RebaseAction.REWORD
        Action.EDIT -> RebaseAction.EDIT
        Action.SQUASH -> RebaseAction.SQUASH
        Action.FIXUP -> RebaseAction.FIXUP
        Action.COMMENT -> RebaseAction.COMMENT
    }
}