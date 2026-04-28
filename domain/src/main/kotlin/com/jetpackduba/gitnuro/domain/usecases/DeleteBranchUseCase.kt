package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class DeleteBranchUseCase @Inject constructor(
    private val deleteBranchGitAction: IDeleteBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(branch: Branch) {
        useCaseExecutor.executeLaunch(
            TaskType.DeleteBranch,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            deleteBranchGitAction(repositoryPath, branch)
        }
    }
}