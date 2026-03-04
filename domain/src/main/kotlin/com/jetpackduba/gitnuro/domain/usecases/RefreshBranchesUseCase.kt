package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.git.branches.GetBranchesGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.GetStatusGitAction
import com.jetpackduba.gitnuro.domain.repositories.AppStateRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshBranchesUseCase @Inject constructor(
    private val getBranchesGitAction: GetBranchesGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val appStateRepository: AppStateRepository,
) {
    suspend fun invoke() {
        val branches = getBranchesGitAction(appStateRepository.repositoryPath!!) // TODO Handle null
        repositoryDataRepository.updateLocalBranches(branches)
    }
}