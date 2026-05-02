package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class DeleteSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val deleteSubmoduleGitAction: IDeleteSubmoduleGitAction,
) {
    operator fun invoke(path: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DeleteSubmodule,
            onRefresh = {
                refreshAllUseCase()
            },
        ) { repositoryPath ->
            deleteSubmoduleGitAction(repositoryPath, path)
        }
    }
}