package com.jetpackduba.gitnuro.observers

import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.data.repositories.BranchesRepository
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class ObserveBranchRefresh @Inject constructor(
    tabState: TabInstanceRepository,
    private val branchesCacheRepository: BranchesRepository,
) : RepositoryChangesObserver(arrayOf(RefreshType.ALL_DATA), tabState) {
    override suspend fun refresh(git: Git) {
        branchesCacheRepository.refresh(git)
    }
}