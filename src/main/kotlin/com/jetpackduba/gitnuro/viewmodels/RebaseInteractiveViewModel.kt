package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.exceptions.InvalidMessageException
import com.jetpackduba.gitnuro.exceptions.RebaseCancelledException
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.rebase.AbortRebaseUseCase
import com.jetpackduba.gitnuro.git.rebase.GetRebaseLinesFullMessageUseCase
import com.jetpackduba.gitnuro.git.rebase.ResumeRebaseInteractiveUseCase
import com.jetpackduba.gitnuro.git.rebase.StartRebaseInteractiveUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RebaseTodoLine.Action
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val TAG = "RebaseInteractiveViewMo"

class RebaseInteractiveViewModel @Inject constructor(
    private val tabState: TabState,
    private val getRebaseLinesFullMessageUseCase: GetRebaseLinesFullMessageUseCase,
    private val startRebaseInteractiveUseCase: StartRebaseInteractiveUseCase,
    private val abortRebaseUseCase: AbortRebaseUseCase,
    private val resumeRebaseInteractiveUseCase: ResumeRebaseInteractiveUseCase,
) {
    private lateinit var commit: RevCommit
    private val _rebaseState = MutableStateFlow<RebaseInteractiveState>(RebaseInteractiveState.Loading)
    val rebaseState: StateFlow<RebaseInteractiveState> = _rebaseState
    var rewordSteps = ArrayDeque<RebaseLine>()

    var onRebaseComplete: () -> Unit = {}

    private var interactiveHandlerContinue = object : InteractiveHandler {
        override fun prepareSteps(steps: MutableList<RebaseTodoLine>) {
            val rebaseState = _rebaseState.value
            if (rebaseState !is RebaseInteractiveState.Loaded) {
                throw Exception("prepareSteps called when rebaseState is not Loaded") // Should never happen, just in case
            }

            val newSteps = rebaseState.stepsList.toMutableList()
            rewordSteps = ArrayDeque(newSteps.filter { it.rebaseAction == RebaseAction.REWORD })

            val newRebaseTodoLines = newSteps
                .filter { it.rebaseAction != RebaseAction.DROP } // Remove dropped lines
                .map { it.toRebaseTodoLine() }

            steps.clear()
            steps.addAll(newRebaseTodoLines)
        }

        override fun modifyCommitMessage(commit: String): String {
            // This can be called when there aren't any reword steps if squash is used.
            val step = rewordSteps.removeLastOrNull() ?: return commit

            val rebaseState = _rebaseState.value
            if (rebaseState !is RebaseInteractiveState.Loaded) {
                throw Exception("modifyCommitMessage called when rebaseState is not Loaded") // Should never happen, just in case
            }

            return rebaseState.messages[step.commit.name()]
                ?: throw InvalidMessageException("Message for commit $commit is unexpectedly null")
        }
    }

    suspend fun startRebaseInteractive(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        showError = true
    ) { git ->
        this@RebaseInteractiveViewModel.commit = revCommit

        val interactiveHandler = object : InteractiveHandler {
            override fun prepareSteps(steps: MutableList<RebaseTodoLine>?) {}
            override fun modifyCommitMessage(message: String?): String = ""
        }

        try {
            val lines = startRebaseInteractiveUseCase(git, interactiveHandler, revCommit, true)
            val messages = getRebaseLinesFullMessageUseCase(tabState.git, lines)
            val rebaseLines = lines.map {
                RebaseLine(
                    it.action.toRebaseAction(),
                    it.commit,
                    it.shortMessage,
                )
            }

            _rebaseState.value = RebaseInteractiveState.Loaded(rebaseLines, messages)

        } catch (ex: Exception) {
            if (ex is RebaseCancelledException) {
                println("Rebase cancelled")
            } else {
                cancel()
                throw ex
            }
        }
    }

    fun continueRebaseInteractive() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        try {
            resumeRebaseInteractiveUseCase(git, interactiveHandlerContinue)
        } finally {
            onRebaseComplete()
        }
    }

    fun onCommitMessageChanged(commit: AbbreviatedObjectId, message: String) {
        val rebaseState = _rebaseState.value

        if (rebaseState !is RebaseInteractiveState.Loaded)
            return

        val messagesMap = rebaseState.messages.toMutableMap()
        messagesMap[commit.name()] = message

        _rebaseState.value = rebaseState.copy(messages = messagesMap)
    }

    fun onCommitActionChanged(commit: AbbreviatedObjectId, rebaseAction: RebaseAction) {
        val rebaseState = _rebaseState.value

        if (rebaseState !is RebaseInteractiveState.Loaded)
            return

        val newStepsList =
            rebaseState.stepsList.toMutableList() // Change the list reference to update the flow with .toList()

        val stepIndex = newStepsList.indexOfFirst {
            it.commit == commit
        }

        if (stepIndex >= 0) {
            val step = newStepsList[stepIndex]
            val newTodoLine = RebaseLine(
                rebaseAction,
                step.commit,
                step.shortMessage
            )

            newStepsList[stepIndex] = newTodoLine

            _rebaseState.value = rebaseState.copy(stepsList = newStepsList)
        }
    }

    fun cancel() = tabState.runOperation(
        refreshType = RefreshType.REPO_STATE
    ) { git ->
        abortRebaseUseCase(git)
    }
}


sealed interface RebaseInteractiveState {
    object Loading : RebaseInteractiveState
    data class Loaded(val stepsList: List<RebaseLine>, val messages: Map<String, String>) : RebaseInteractiveState
    data class Failed(val error: String) : RebaseInteractiveState
}

data class RebaseLine(
    val rebaseAction: RebaseAction,
    val commit: AbbreviatedObjectId,
    val shortMessage: String
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