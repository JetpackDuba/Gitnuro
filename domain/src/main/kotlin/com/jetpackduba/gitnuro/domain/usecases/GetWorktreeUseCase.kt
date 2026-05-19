package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetWorktreePathGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class GetWorktreeUseCase @Inject constructor(
    private val getWorktreePathGitAction: IGetWorktreePathGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(): Either<String, AppError> {
        return useCaseExecutor.execute(
            taskType = TaskType.GetWorktree,
        ) { repositoryPath ->
            getWorktreePathGitAction(repositoryPath)
        }
    }
}