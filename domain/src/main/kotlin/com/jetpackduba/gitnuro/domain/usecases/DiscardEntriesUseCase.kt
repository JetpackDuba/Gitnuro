package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDiscardEntriesGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class DiscardEntriesUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val discardEntriesGitAction: IDiscardEntriesGitAction,
) {
    operator fun invoke(statusEntries: List<StatusEntry>, isStaged: Boolean) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DiscardFile,
            dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG),
        ) { repositoryPath ->
            discardEntriesGitAction(repositoryPath, statusEntries, isStaged)
        }
    }
}
