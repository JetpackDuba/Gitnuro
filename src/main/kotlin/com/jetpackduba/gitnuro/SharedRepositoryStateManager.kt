package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.rebase.GetRebaseInteractiveStateUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.git.repository.GetRepositoryStateUseCase
import com.jetpackduba.gitnuro.logging.printLog
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
    private val tabState: TabState,
    private val getRebaseInteractiveStateUseCase: GetRebaseInteractiveStateUseCase,
    private val getRepositoryStateUseCase: GetRepositoryStateUseCase,
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
        _repositoryState.value = getRepositoryStateUseCase(git)
    }

    private suspend fun updateRebaseInteractiveState(git: Git) {
        val newRepositoryState = getRebaseInteractiveStateUseCase(git)
        printLog(TAG, "Refreshing repository state $newRepositoryState")

        _rebaseInteractiveState.value = newRepositoryState
    }
}