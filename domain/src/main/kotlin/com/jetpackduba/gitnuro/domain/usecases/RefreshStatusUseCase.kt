package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetStatusGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshStatusUseCase @Inject constructor(
    private val getStatusGitAction: IGetStatusGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke() {
        val repository = repositoryDataRepository.repositoryPath ?: return
        val status = getStatusGitAction(repository)

        if (status is Either.Ok) {
            repositoryDataRepository.updateStatus(status.value)
        }
    }
}