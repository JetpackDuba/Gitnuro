package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IRenameBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class RenameBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val renameBranchGitAction: IRenameBranchGitAction,
    private val refreshBranchUseCase: RefreshBranchesUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val setTrackingBranchGitAction: ISetTrackingBranchGitAction,
) {
    suspend operator fun invoke(oldName: String, newName: String): Either<Unit, AppError> {
        return useCaseExecutor.execute(
            taskType = TaskType.RenameBranch,
            onRefresh = {
                refreshBranchUseCase()
                refreshLogUseCase()
            },
        ) { repositoryPath ->
            val branch = renameBranchGitAction(repositoryPath, oldName, newName).bind()

            setTrackingBranchGitAction(repositoryPath, branch, null, null)
        }
    }
}