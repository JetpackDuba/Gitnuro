package app.viewmodels

import app.exceptions.InvalidMessageException
import app.exceptions.RebaseCancelledException
import app.git.RebaseManager
import app.git.RefreshType
import app.git.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RebaseTodoLine.Action
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class RebaseInteractiveViewModel @Inject constructor(
    private val tabState: TabState,
    private val rebaseManager: RebaseManager,
) {
    private val rebaseInteractiveMutex = Mutex(true)
    private val _rebaseState = MutableStateFlow<RebaseInteractiveState>(RebaseInteractiveState.Loading)
    val rebaseState: StateFlow<RebaseInteractiveState> = _rebaseState
    var rewordSteps = ArrayDeque<RebaseTodoLine>()

    private var cancelled = false
    private var completed = false

    private var interactiveHandler = object : InteractiveHandler {
        override fun prepareSteps(steps: MutableList<RebaseTodoLine>) = runBlocking {
            println("prepareSteps started")
            tabState.refreshData(RefreshType.REPO_STATE)

            tabState.coRunOperation(refreshType = RefreshType.NONE) { git ->
                val messages = rebaseManager.rebaseLinesFullMessage(git, steps)

                _rebaseState.value = RebaseInteractiveState.Loaded(steps, messages)
            }

            println("prepareSteps mutex lock")
            rebaseInteractiveMutex.lock()

            if (cancelled) {
                throw RebaseCancelledException("Rebase cancelled due to user request")
            }

            val rebaseState = _rebaseState.value
            if (rebaseState !is RebaseInteractiveState.Loaded) {
                throw Exception("prepareSteps called when rebaseState is not Loaded") // Should never happen, just in case
            }

            val newSteps = rebaseState.stepsList
            rewordSteps = ArrayDeque(newSteps.filter { it.action == Action.REWORD })

            steps.clear()
            steps.addAll(newSteps)
            println("prepareSteps finished")
        }

        override fun modifyCommitMessage(commit: String): String = runBlocking {
            // This can be called when there aren't any reword steps if squash is used.
            val step = rewordSteps.removeLastOrNull() ?: return@runBlocking commit

            val rebaseState = _rebaseState.value
            if (rebaseState !is RebaseInteractiveState.Loaded) {
                throw Exception("modifyCommitMessage called when rebaseState is not Loaded") // Should never happen, just in case
            }

            return@runBlocking rebaseState.messages[step.commit.name()]
                ?: throw InvalidMessageException("Message for commit $commit is unexpectedly null")
        }
    }

    suspend fun startRebaseInteractive(revCommit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.ALL_DATA,
        showError = true
    ) { git ->
        try {
            rebaseManager.rebaseInteractive(git, interactiveHandler, revCommit)
            completed = true
        } catch (ex: Exception) {
            if (ex is RebaseCancelledException) {
                println("Rebase cancelled")
            } else {
                cancel()
                throw ex
            }
        }
    }

    fun continueRebaseInteractive() = tabState.runOperation(
        refreshType = RefreshType.ONLY_LOG,
    ) {
        rebaseInteractiveMutex.unlock()
    }

    fun onCommitMessageChanged(commit: AbbreviatedObjectId, message: String) {
        val rebaseState = _rebaseState.value

        if (rebaseState !is RebaseInteractiveState.Loaded)
            return

        val messagesMap = rebaseState.messages.toMutableMap()
        messagesMap[commit.name()] = message

        _rebaseState.value = rebaseState.copy(messages = messagesMap)
    }

    fun onCommitActionChanged(commit: AbbreviatedObjectId, action: Action) {
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
            val newTodoLine = RebaseTodoLine(action, step.commit, step.shortMessage)
            newStepsList[stepIndex] = newTodoLine

            _rebaseState.value = rebaseState.copy(stepsList = newStepsList)
        }
    }

    fun cancel() = tabState.runOperation(
        refreshType = RefreshType.REPO_STATE
    ) { git ->
        if (!cancelled && !completed) {
            rebaseManager.abortRebase(git)

            cancelled = true

            rebaseInteractiveMutex.unlock()
        }
    }

    fun resumeRebase() = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        try {
            rebaseManager.resumeRebase(git, interactiveHandler)
            completed = true
        } catch (ex: Exception) {
            if (ex is RebaseCancelledException) {
                println("Rebase cancelled")
            } else {
                cancel()
                throw ex
            }
        }
    }
}


sealed interface RebaseInteractiveState {
    object Loading : RebaseInteractiveState
    data class Loaded(val stepsList: List<RebaseTodoLine>, val messages: Map<String, String>) : RebaseInteractiveState
    data class Failed(val error: String) : RebaseInteractiveState
}