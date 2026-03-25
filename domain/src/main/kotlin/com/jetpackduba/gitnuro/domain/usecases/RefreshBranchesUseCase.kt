package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.interfaces.IGetBranchesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import javax.inject.Inject

class RefreshBranchesUseCase @Inject constructor(
    private val getBranchesGitAction: IGetBranchesGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke() {
        val repository = repositoryDataRepository.repositoryPath ?: return

        val branches = getBranchesGitAction(repository).okOrNull() ?: return
        repositoryDataRepository.updateLocalBranches(branches)

        val currentBranch = getCurrentBranchGitAction(repository).okOrNull() ?: return
        repositoryDataRepository.updateCurrentBranch(currentBranch)
    }
}