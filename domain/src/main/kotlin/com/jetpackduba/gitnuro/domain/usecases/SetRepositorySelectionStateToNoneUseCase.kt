package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class SetRepositorySelectionStateToNoneUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    operator fun invoke() {
        repositoryDataRepository.setRepositoryState(RepositorySelectionState.None)
    }
}