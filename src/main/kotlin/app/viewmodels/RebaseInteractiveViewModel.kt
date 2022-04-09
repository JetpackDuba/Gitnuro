package app.viewmodels

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
            _rebaseState.value = RebaseInteractiveState.Loaded(steps?.reversed() ?: emptyList())
        }

        override fun modifyCommitMessage(commit: String?): String {
            return commit.orEmpty() // we don't care about this since it's not called
        }
    }

    fun startRebaseInteractive() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        rebaseManager.rebaseInteractive(git, interactiveHandler, revCommit)
    }

    fun continueRebaseInteractive() = tabState.runOperation(
        refreshType = RefreshType.ONLY_LOG,
    ) { git ->
        rebaseManager.rebaseInteractive(
            git = git,
            interactiveHandler = object : InteractiveHandler {
                override fun prepareSteps(steps: MutableList<RebaseTodoLine>?) {
                    val rebaseState = _rebaseState.value

                    if(rebaseState !is RebaseInteractiveState.Loaded)
                        return

                    val newSteps = rebaseState.stepsList

                    for (step in steps ?: emptyList()) {
                        val foundStep = newSteps.firstOrNull { it.commit.name() == step.commit.name() }

                        if (foundStep != null) {
                            step.action = foundStep.action
                        }
                    }
                }

                override fun modifyCommitMessage(commit: String?): String {
                    return commit.orEmpty()
                }
            },
            commit = revCommit
        )
    }

    fun onCommitActionChanged(commit: AbbreviatedObjectId, action: Action) {
        val rebaseState = _rebaseState.value

        if(rebaseState !is RebaseInteractiveState.Loaded)
            return

        val newStepsList = rebaseState.stepsList.toMutableList() // Change the list reference to update the flow with .toList()

        val stepIndex = newStepsList.indexOfFirst {
            it.commit == commit
        }

        if (stepIndex >= 0) {
            val step = newStepsList[stepIndex]
            val newTodoLine = RebaseTodoLine(action, step.commit, step.shortMessage)
            newStepsList[stepIndex] = newTodoLine

            _rebaseState.value = RebaseInteractiveState.Loaded(newStepsList)
        }
    }
}


sealed interface RebaseInteractiveState {
    object Loading : RebaseInteractiveState
    data class Loaded(val stepsList: List<RebaseTodoLine>) : RebaseInteractiveState
    data class Failed(val error: String) : RebaseInteractiveState
    object Finished : RebaseInteractiveState
}