package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.git.rebase.GetRebaseInteractiveStateGitAction
import com.jetpackduba.gitnuro.domain.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.git.repository.GetRepositoryStateGitAction
import com.jetpackduba.gitnuro.common.printLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

private const val TAG = "SharedRepositoryStateMa"

@TabScope
class SharedRepositoryStateManager @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val getRebaseInteractiveStateGitAction: com.jetpackduba.gitnuro.domain.git.rebase.GetRebaseInteractiveStateGitAction,
    private val getRepositoryStateGitAction: com.jetpackduba.gitnuro.domain.git.repository.GetRepositoryStateGitAction,
    tabScope: CoroutineScope,
) {
    private val _repositoryState = MutableStateFlow(RepositoryState.SAFE)
    val repositoryState = _repositoryState.asStateFlow()

    private val _rebaseInteractiveState = MutableStateFlow<com.jetpackduba.gitnuro.domain.git.rebase.RebaseInteractiveState>(
        _root_ide_package_.com.jetpackduba.gitnuro.domain.git.rebase.RebaseInteractiveState.None)
    val rebaseInteractiveState = _rebaseInteractiveState.asStateFlow()

    init {
        tabScope.apply {
            launch {
                tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.REBASE_INTERACTIVE_STATE) {
                    updateRebaseInteractiveState(tabState.git)
                }
            }
            launch {
                tabState.refreshFlowFiltered(
                    RefreshType.ALL_DATA,
                    RefreshType.REPO_STATE,
                    RefreshType.UNCOMMITTED_CHANGES,
                    RefreshType.UNCOMMITTED_CHANGES_AND_LOG
                ) {
                    updateRepositoryState(tabState.git)
                }
            }
        }
    }

    private suspend fun updateRepositoryState(git: Git) {
        _repositoryState.value = getRepositoryStateGitAction(git)
    }

    private suspend fun updateRebaseInteractiveState(git: Git) {
        val newRepositoryState = getRebaseInteractiveStateGitAction(git)
        printLog(TAG, "Refreshing repository state $newRepositoryState")

        _rebaseInteractiveState.value = newRepositoryState
    }
}