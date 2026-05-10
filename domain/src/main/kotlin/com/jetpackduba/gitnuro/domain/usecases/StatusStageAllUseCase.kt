package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IStageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

private const val TAG = "StatusStageAllUseCase"

class StatusStageAllUseCase @Inject constructor(
    private val stageAllGitAction: IStageAllGitAction,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(entries: List<StatusEntry>?) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.StageAllFiles,
            onRefresh = { refreshStatusUseCase() }
        ) { repositoryPath ->
            stageAllGitAction(repositoryPath, entries)
        }
    }
}
