package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IStageByDirectoryGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class StageByDirectoryUseCase @Inject constructor(
    private val stageByDirectoryGitAction: IStageByDirectoryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(dir: String) = useCaseExecutor.executeLaunch(
        taskType = TaskType.StageDir,
        dataToRefresh = arrayOf(DataToRefresh.STATUS),
    ) { repositoryPath ->
        stageByDirectoryGitAction(repositoryPath, dir)
    }
}