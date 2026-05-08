package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.exceptions.InvalidMessageException
import com.jetpackduba.gitnuro.domain.exceptions.RebaseCancelledException
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.AbortRebaseUseCase
import com.jetpackduba.gitnuro.domain.usecases.GetCommitFromRebaseLineUseCase
import com.jetpackduba.gitnuro.domain.usecases.GetRepositoryStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RebaseTodoLine.Action
import javax.inject.Inject

private const val TAG = "RebaseInteractiveViewMo"

class RebaseInteractiveViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
//    private val getRebaseLinesFullMessageGitAction: IGetRebaseLinesFullMessageGitAction,
    private val getCommitFromRebaseLineUseCase: GetCommitFromRebaseLineUseCase,
    private val getRebaseInteractiveTodoLinesGitAction: IGetRebaseInteractiveTodoLinesGitAction,
    private val abortRebaseUseCase: AbortRebaseUseCase,
    private val resumeRebaseInteractiveGitAction: IResumeRebaseInteractiveGitAction,
    private val getRepositoryStateUseCase: GetRepositoryStateUseCase,
) : TabViewModel() {
    private val _rebaseState = MutableStateFlow<RebaseInteractiveViewState>(RebaseInteractiveViewState.Loading)
    val rebaseState: StateFlow<RebaseInteractiveViewState> = _rebaseState

    val selectedItem = tabState.selectedItem
    var rewordSteps = ArrayDeque<RebaseLine>()

    private var interactiveHandlerContinue = object : InteractiveHandler {
        override fun prepareSteps(steps: MutableList<RebaseTodoLine>) {
            val rebaseState = _rebaseState.value
            if (rebaseState !is RebaseInteractiveViewState.Loaded) {
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
            val step = rewordSteps.removeFirstOrNull() ?: return commit

            val rebaseState = _rebaseState.value
            if (rebaseState !is RebaseInteractiveViewState.Loaded) {
                throw Exception("modifyCommitMessage called when rebaseState is not Loaded") // Should never happen, just in case
            }

            return rebaseState.messages[step.commit.name()]
                ?: throw InvalidMessageException("Message for commit $commit is unexpectedly null")
        }
    }

    fun loadRebaseInteractiveData() {
        // TODO Move this to use case
//        viewModelScope.launch {
//            val stateResult = getRepositoryStateUseCase()
//
//            when (stateResult) {
//                is Either.Err -> {
//                    _rebaseState.value = RebaseInteractiveViewState.Failed(stateResult.error.toString())
//                }
//
//                is Either.Ok -> {
//                    val state = stateResult.value
//                    if (!state.isRebasing) {
//                        _rebaseState.value = RebaseInteractiveViewState.Loading
//                        return@launch
//                    }
//                }
//            }
//        }
//
//
//
//        try {
//            val lines = getRebaseInteractiveTodoLinesGitAction(git)
//            val messages = getRebaseLinesFullMessageGitAction(tabState.git, lines)
//            val rebaseLines = lines.map {
//                RebaseLine(
//                    it.action.toRebaseAction(),
//                    it.commit,
//                    it.shortMessage,
//                )
//            }
//
//            val isSameRebase = isSameRebase(rebaseLines, _rebaseState.value)
//
//            if (!isSameRebase) {
//                _rebaseState.value = RebaseInteractiveViewState.Loaded(rebaseLines, messages)
//                val firstLine = rebaseLines.firstOrNull()
//
//                if (firstLine != null) {
//                    val fullCommit = getCommitFromRebaseLineUseCase(firstLine.commit, firstLine.shortMessage)
//                    tabState.newSelectedCommit(fullCommit)
//                }
//            }
//
//        } catch (ex: Exception) {
//            if (ex is RebaseCancelledException) {
//                println("Rebase cancelled")
//            } else {
//                cancel()
//                throw ex
//            }
//        }
//
//        null
    }

    private fun isSameRebase(rebaseLines: List<RebaseLine>, state: RebaseInteractiveViewState): Boolean {
        if (state is RebaseInteractiveViewState.Loaded) {
            val stepsList = state.stepsList

            if (rebaseLines.count() != stepsList.count()) {
                return false
            }

            return rebaseLines.map { it.commit.name() } == stepsList.map { it.commit.name() }
        }

        return false
    }

    fun continueRebaseInteractive() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RebaseInteractive, // TODO Perhaps be more precise with the task type
    ) { git ->
        resumeRebaseInteractiveGitAction(git, interactiveHandlerContinue)
        _rebaseState.value = RebaseInteractiveViewState.Loading

        null
    }

    fun onCommitMessageChanged(commit: AbbreviatedObjectId, message: String) {
        val rebaseState = _rebaseState.value

        if (rebaseState !is RebaseInteractiveViewState.Loaded)
            return

        val messagesMap = rebaseState.messages.toMutableMap()
        messagesMap[commit.name()] = message

        _rebaseState.value = rebaseState.copy(messages = messagesMap)
    }

    fun onCommitActionChanged(commit: AbbreviatedObjectId, rebaseAction: RebaseAction) {
        val rebaseState = _rebaseState.value

        if (rebaseState !is RebaseInteractiveViewState.Loaded)
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

    fun cancel() {
        abortRebaseUseCase()
        _rebaseState.value = RebaseInteractiveViewState.Loading
    }

    fun selectLine(line: RebaseLine) = viewModelScope.launch {
        val fullCommit = getCommitFromRebaseLineUseCase(TODO()/*line.commit*/, line.shortMessage)
        tabState.newSelectedCommit(TODO()/*line.commit*/)

        null
    }

    fun moveCommit(from: Int, to: Int) {
        val state = _rebaseState.value

        if (state is RebaseInteractiveViewState.Loaded) {

            val newStepsList = state.stepsList.toMutableList().apply {
                add(to, removeAt(from))
            }

            _rebaseState.value = state.copy(stepsList = newStepsList)
        }
    }
}


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