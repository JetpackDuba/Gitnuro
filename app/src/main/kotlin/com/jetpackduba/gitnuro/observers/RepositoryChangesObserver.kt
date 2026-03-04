package com.jetpackduba.gitnuro.observers

import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.api.Git

abstract class RepositoryChangesObserver(
    private val refreshType: Array<RefreshType>,
    private val tabState: TabInstanceRepository,
) {
    suspend fun startObserving() {
        tabState.refreshFlowFiltered(*refreshType) {
            refresh(tabState.git)
        }
    }

    abstract suspend fun refresh(git: Git)
}