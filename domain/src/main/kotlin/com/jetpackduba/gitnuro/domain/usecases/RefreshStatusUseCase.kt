package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.git.workspace.GetStatusGitAction
import com.jetpackduba.gitnuro.domain.repositories.AppStateRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshStatusUseCase @Inject constructor(
    private val getStatusGitAction: GetStatusGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val appStateRepository: AppStateRepository,
) {
    suspend fun invoke() {
        val status = getStatusGitAction(appStateRepository.repositoryPath!!) // TODO Handle null
        repositoryDataRepository.updateStatus(status)
    }
}