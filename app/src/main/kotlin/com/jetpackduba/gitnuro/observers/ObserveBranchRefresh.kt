package com.jetpackduba.gitnuro.observers

import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.RefreshBranchesUseCase
import org.eclipse.jgit.api.Git
import javax.inject.Inject

// TODO Remove this class
class ObserveBranchRefresh @Inject constructor(
    tabState: TabInstanceRepository,
    private val refreshBranchesUseCase: RefreshBranchesUseCase,
) : RepositoryChangesObserver(arrayOf(RefreshType.ALL_DATA), tabState) {
    override suspend fun refresh(git: Git) {

    }
}