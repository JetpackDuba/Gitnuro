package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteTagGitAction
import com.jetpackduba.gitnuro.domain.models.Tag
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class DeleteTagUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val deleteTagGitAction: IDeleteTagGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(tag: Tag) = useCaseExecutor.executeLaunch(
        taskType = TaskType.CreateTag,
        onRefresh = {
            refreshAllUseCase()
        }
    ) { repositoryPath ->
        deleteTagGitAction(repositoryPath, tag)
    }
}