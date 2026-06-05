package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.ICreateTagGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class CreateTagUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val createTagGitAction: ICreateTagGitAction,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val refreshTagsUseCase: RefreshTagsUseCase,
) {
    operator fun invoke(tag: String, revCommit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CreateTag,
            onRefresh = {
                refreshLogUseCase()
                refreshTagsUseCase()
            }
        ) { repositoryPath ->
            createTagGitAction(repositoryPath, tag, revCommit)
        }
    }
}