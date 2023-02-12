package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.GetBranchByCommitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class SquashCommitsViewModel @Inject constructor(
    private val tabState: TabState,
    private val rebaseInteractiveViewModel: RebaseInteractiveViewModel,
    private val getBranchByCommitUseCase: GetBranchByCommitUseCase,
) {
    private val _squashState = MutableStateFlow<SquashCommitsState>(SquashCommitsState.Loading)
    val squashState: StateFlow<SquashCommitsState> = _squashState

    private var commits: List<RevCommit> = emptyList()
    private var squashMessage: String? = null

    init {
        tabState.runOperation(
            refreshType = RefreshType.NONE
        ) {
            rebaseInteractiveViewModel.rebaseState.collect { state ->
                _squashState.value = when (state) {
                    is RebaseInteractiveState.Failed -> SquashCommitsState.Failed(state.error)
                    is RebaseInteractiveState.Loading -> SquashCommitsState.Loading
                    is RebaseInteractiveState.Loaded -> {
                        SquashCommitsState.Loaded(
                            message = getDefaultSquashMessageFromCommits(
                                messages = commits.mapNotNull { state.messages[it.abbreviate(7).name()] }
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun startSquash(commits: List<RevCommit>, upstreamCommit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.ALL_DATA,
        showError = true,
    ) { git ->
        commits
            .groupBy { getBranchByCommitUseCase(git, it) }
            .let {
                if (it.size > 1) throw Exception("Squash is impossible")
            }

        this.commits = commits

        rebaseInteractiveViewModel.startRebaseInteractive(upstreamCommit)
    }

    fun cancel() {
        rebaseInteractiveViewModel.cancel()
    }

    fun editMessage(message: String) {
        val state = _squashState.value
        if (state !is SquashCommitsState.Loaded) return
        _squashState.value = state.copy(message = message)
        squashMessage = message
    }

    fun continueSquash() {
        commits.forEachIndexed { index, commit ->
            val abbreviate = commit.abbreviate(7)
            val action = if (index == commits.size - 1) RebaseTodoLine.Action.PICK else RebaseTodoLine.Action.SQUASH
            rebaseInteractiveViewModel.onCommitActionChanged(abbreviate, action)
        }

        rebaseInteractiveViewModel.continueRebaseInteractive()
    }

    private fun getDefaultSquashMessageFromCommits(messages: List<String>): String = buildString {
        messages.forEachIndexed { index, value ->
            append(value)
            if (messages.size != index + 1) {
                append("\n")
            }
        }
    }

}

sealed interface SquashCommitsState {
    object Loading : SquashCommitsState
    data class Loaded(val message: String) : SquashCommitsState
    data class Failed(val error: String) : SquashCommitsState
}