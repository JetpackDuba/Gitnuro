package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IUpdateSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class UpdateSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val updateSubmoduleGitAction: IUpdateSubmoduleGitAction,
) {
    operator fun invoke(path: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.UpdateSubmodule,
            onRefresh = {
                refreshAllUseCase()
            },
        ) { repositoryPath ->
            updateSubmoduleGitAction(repositoryPath, path)
        }
    }
}