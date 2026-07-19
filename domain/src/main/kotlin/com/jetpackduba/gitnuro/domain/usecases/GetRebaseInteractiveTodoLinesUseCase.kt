package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveTodoLinesGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseLine
import javax.inject.Inject

class GetRebaseInteractiveTodoLinesUseCase @Inject constructor(
    private val getRebaseInteractiveTodoLinesGitAction: IGetRebaseInteractiveTodoLinesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(): Either<List<RebaseLine>, AppError> {
        return useCaseExecutor.execute(
        ) { repositoryPath ->
            getRebaseInteractiveTodoLinesGitAction(repositoryPath)
        }
    }
}