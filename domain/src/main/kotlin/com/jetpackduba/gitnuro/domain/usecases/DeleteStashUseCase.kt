package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class DeleteStashUseCase @Inject constructor(
    private val deleteStashGitAction: IDeleteStashGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(stash: Commit) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Stash,
        refreshEvenIfFailed = true,
        dataToRefresh = arrayOf(DataToRefresh.STASHES, DataToRefresh.LOG),
    ) { repositoryPath ->
        deleteStashGitAction(repositoryPath, stash)
    }
}