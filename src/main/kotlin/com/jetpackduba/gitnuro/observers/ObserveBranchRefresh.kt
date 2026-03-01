package com.jetpackduba.gitnuro.observers

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.repositories.BranchesRepository
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class ObserveBranchRefresh @Inject constructor(
    tabState: TabState,
    private val branchesCacheRepository: BranchesRepository,
) : RepositoryChangesObserver(arrayOf(RefreshType.ALL_DATA), tabState) {
    override suspend fun refresh(git: Git) {
        branchesCacheRepository.refresh(git)
    }
}