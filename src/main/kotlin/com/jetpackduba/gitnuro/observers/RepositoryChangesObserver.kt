package com.jetpackduba.gitnuro.observers

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import org.eclipse.jgit.api.Git

abstract class RepositoryChangesObserver(
    private val refreshType: Array<RefreshType>,
    private val tabState: TabState,
) {
    suspend fun startObserving() {
        tabState.refreshFlowFiltered(*refreshType) {
            refresh(tabState.git)
        }
    }

    abstract suspend fun refresh(git: Git)
}