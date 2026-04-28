package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.ILoadAuthorGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class GetAuthorUseCase @Inject constructor(
    private val loadAuthorGitAction: ILoadAuthorGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke() = useCaseExecutor.execute(taskType = TaskType.LoadAuthor) { repositoryPath ->
        loadAuthorGitAction(repositoryPath)
    }
}