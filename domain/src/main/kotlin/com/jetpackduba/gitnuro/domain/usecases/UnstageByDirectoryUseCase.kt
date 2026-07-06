package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageByDirectoryGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class UnstageByDirectoryUseCase @Inject constructor(
    private val unstageByDirectoryGitAction: IUnstageByDirectoryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(dir: String) = useCaseExecutor.executeLaunch(
        taskType = TaskType.StageDir,
        dataToRefresh = arrayOf(DataToRefresh.STATUS),
    ) { repositoryPath ->
        unstageByDirectoryGitAction(repositoryPath, dir)
    }
}