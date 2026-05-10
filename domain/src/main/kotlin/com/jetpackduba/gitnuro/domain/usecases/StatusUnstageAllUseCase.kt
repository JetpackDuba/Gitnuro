package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class StatusUnstageAllUseCase @Inject constructor(
    private val unstageAllGitAction: IUnstageAllGitAction,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(statusEntries: List<StatusEntry>?) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.UnstageAllFiles,
            onRefresh = { refreshStatusUseCase() }
        ) { repositoryPath ->
            unstageAllGitAction(repositoryPath, statusEntries)
        }
    }
}
