package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveTodoLinesGitAction
import javax.inject.Inject

class GetRebaseInteractiveTodoLinesUseCase @Inject constructor(
    private val getRebaseInteractiveTodoLinesGitAction: IGetRebaseInteractiveTodoLinesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke()  {
        useCaseExecutor.execute(
        ) { repositoryPath ->
            getRebaseInteractiveTodoLinesGitAction(repositoryPath)
        }
    }
}