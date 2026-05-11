package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetBranchesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshBranchesUseCase @Inject constructor(
    private val getBranchesGitAction: IGetBranchesGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke() {
        useCaseExecutor.executeOnTabScope(TaskType.RefreshBranches) { repositoryPath ->
            val branches = getBranchesGitAction(repositoryPath).bind()
            repositoryDataRepository.updateLocalBranches(branches)

            val currentBranch = getCurrentBranchGitAction(repositoryPath).bind()
            repositoryDataRepository.updateCurrentBranch(currentBranch)
        }
    }
}