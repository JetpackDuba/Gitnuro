package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IAddSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class AddSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val addSubmoduleGitAction: IAddSubmoduleGitAction,
) {
    operator fun invoke(name: String, path: String, uri: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.AddSubmodule,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            addSubmoduleGitAction(repositoryPath, name, path, uri)
        }
    }
}