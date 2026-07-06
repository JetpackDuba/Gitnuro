package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.ISyncSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class SyncSubmoduleUseCase @Inject constructor(
    private val syncSubmoduleGitAction: ISyncSubmoduleGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(submodulePath: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.SyncSubmodule,
            dataToRefresh = arrayOf(DataToRefresh.SUBMODULES),
        ) { repository ->
            syncSubmoduleGitAction(repository, submodulePath)
        }
    }
}