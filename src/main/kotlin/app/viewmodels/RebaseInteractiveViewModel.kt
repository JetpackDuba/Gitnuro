package app.viewmodels

import app.exceptions.InvalidMessageException
import app.git.RebaseManager
import app.git.RefreshType
import app.git.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    lateinit var revCommit: RevCommit

    private val _rebaseState = MutableStateFlow<RebaseInteractiveState>(RebaseInteractiveState.Loading)
    val rebaseState: StateFlow<RebaseInteractiveState> = _rebaseState

    private var interactiveHandler = object : InteractiveHandler {
        override fun prepareSteps(steps: MutableList<RebaseTodoLine>?) {
            _rebaseState.value = RebaseInteractiveState.Loaded(steps?.reversed() ?: emptyList(), emptyMap())
        }

        override fun modifyCommitMessage(commit: String?): String {
            return commit.orEmpty() // we don't care about this since it's not called
        }
    }

    fun startRebaseInteractive() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        rebaseManager.rebaseInteractive(git, interactiveHandler, revCommit)

        val rebaseState = _rebaseState.value

        if (rebaseState is RebaseInteractiveState.Loaded) {
            val messages = rebaseManager.rebaseLinesFullMessage(git, rebaseState.stepsList, revCommit)
            _rebaseState.value = rebaseState.copy(messages = messages)
        }
    }

    fun continueRebaseInteractive() = tabState.runOperation(
        refreshType = RefreshType.ONLY_LOG,
    ) { git ->
        val rebaseState = _rebaseState.value
        if (rebaseState !is RebaseInteractiveState.Loaded) {
            println("continueRebaseInteractive called when rebaseState is not Loaded")
            return@runOperation // Should never happen, just in case
        }

        val newSteps = rebaseState.stepsList
        val rewordSteps = ArrayDeque<RebaseTodoLine>(newSteps.filter { it.action == Action.REWORD })

        rebaseManager.rebaseInteractive(
            git = git,
            interactiveHandler = object : InteractiveHandler {
                override fun prepareSteps(steps: MutableList<RebaseTodoLine>?) {
                    for (step in steps ?: emptyList()) {
                        val foundStep = newSteps.firstOrNull { it.commit.name() == step.commit.name() }

                        if (foundStep != null) {
                            step.action = foundStep.action
                        }
                    }
                }

                override fun modifyCommitMessage(commit: String): String {
                    // This can be called when there aren't any reword steps if squash is used.
                    val step = rewordSteps.removeLastOrNull() ?: return commit

                    return rebaseState.messages[step.commit.name()]
                        ?: throw InvalidMessageException("Message for commit $commit is unexpectedly null")
                }
            },
            commit = revCommit
        )
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
}


sealed interface RebaseInteractiveState {
    object Loading : RebaseInteractiveState
    data class Loaded(val stepsList: List<RebaseTodoLine>, val messages: Map<String, String>) : RebaseInteractiveState
    data class Failed(val error: String) : RebaseInteractiveState
    object Finished : RebaseInteractiveState
}