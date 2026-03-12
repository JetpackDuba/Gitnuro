package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IGetBranchesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.repositories.AppStateRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshBranchesUseCase @Inject constructor(
    private val getBranchesGitAction: IGetBranchesGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val appStateRepository: AppStateRepository,
) {
    suspend fun invoke() {
        val repository = appStateRepository.repositoryPath ?: return

        val branches = getBranchesGitAction(repository)
        repositoryDataRepository.updateLocalBranches(branches)

        val currentBranch = getCurrentBranchGitAction(repository)
        repositoryDataRepository.updateCurrentBranch(currentBranch)
    }
}