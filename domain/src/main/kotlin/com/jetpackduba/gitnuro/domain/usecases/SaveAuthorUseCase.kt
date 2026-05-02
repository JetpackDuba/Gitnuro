package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.ISaveAuthorGitAction
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class SaveAuthorUseCase @Inject constructor(
    private val saveAuthorGitAction: ISaveAuthorGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(authorInfo: AuthorInfo) {
        // TODO This should be "execute" and the UI should handle the error
        useCaseExecutor.executeLaunch(
            taskType = TaskType.SaveAuthor,
            onRefresh = {
                // TODO refresh repository state
            }
        ) { repositoryPath ->
            saveAuthorGitAction(repositoryPath, authorInfo)
        }
    }
}