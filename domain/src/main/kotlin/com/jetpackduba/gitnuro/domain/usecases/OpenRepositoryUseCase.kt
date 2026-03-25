package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IOpenRepositoryGitAction
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class OpenRepositoryUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val openRepositoryGitAction: IOpenRepositoryGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    suspend operator fun invoke(directory: String) {
        val repositoryPath = openRepositoryGitAction(directory)

        if (repositoryPath != null) {
            repositoryDataRepository.setRepositoryState(RepositorySelectionState.Open(repositoryPath))
            refreshAllUseCase()
        } else {
            // TODO Add error to ErrorRepository?
            repositoryDataRepository.setRepositoryState(RepositorySelectionState.None)
        }

    }
}