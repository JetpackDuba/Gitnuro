package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
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
    private val getRebaseInteractiveStateGitAction: IGetRebaseInteractiveStateGitAction,
    private val getRepositoryStateGitAction: IGetRepositoryStateGitAction,
    tabScope: CoroutineScope,
) {
    private val _repositoryState = MutableStateFlow(RepositoryState.SAFE)
    val repositoryState = _repositoryState.asStateFlow()

    private val _rebaseInteractiveState = MutableStateFlow<RebaseInteractiveState>(RebaseInteractiveState.None)
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