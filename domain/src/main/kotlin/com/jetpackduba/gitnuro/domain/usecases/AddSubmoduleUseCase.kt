package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IAddSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class AddSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val addSubmoduleGitAction: IAddSubmoduleGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(name: String, path: String, uri: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.AddSubmodule,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            addSubmoduleGitAction(repositoryPath, name, path, uri)
        }
    }
}