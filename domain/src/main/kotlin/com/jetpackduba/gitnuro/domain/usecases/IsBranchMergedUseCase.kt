package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IIsBranchMergedGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class IsBranchMergedUseCase @Inject constructor(
    private val isBranchMergedGitAction: IIsBranchMergedGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(branch: Branch): Either<Boolean, AppError> {
        return useCaseExecutor.executeLaunchAsync(
            taskType = TaskType.CheckingBranchMerged,
            dataToRefresh = emptyArray(),
            includeErrorHandling = false
        ) { repositoryPath ->
            isBranchMergedGitAction(repositoryPath, branch)
        }.await()
    }
}