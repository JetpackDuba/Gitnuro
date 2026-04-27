package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IStageByDirectoryGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class StageByDirectoryUseCase @Inject constructor(
    private val stageByDirectoryGitAction: IStageByDirectoryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshStatusUseCase: RefreshStatusUseCase,
) {
    operator fun invoke(dir: String) = useCaseExecutor.executeLaunch(
        taskType = TaskType.STAGE_DIR,
        onRefresh = {
            refreshStatusUseCase()
        }
    ) { repositoryPath ->
        stageByDirectoryGitAction(repositoryPath, dir)
    }
}