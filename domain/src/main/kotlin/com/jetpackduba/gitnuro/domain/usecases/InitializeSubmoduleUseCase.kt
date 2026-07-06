package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IInitializeSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IUpdateSubmoduleGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class InitializeSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val initializeSubmoduleGitAction: IInitializeSubmoduleGitAction,
    private val updateSubmoduleGitAction: IUpdateSubmoduleGitAction,
) {
    operator fun invoke(path: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.InitSubmodule,
            dataToRefresh = arrayOf(DataToRefresh.SUBMODULES),
        ) { repositoryPath ->
            initializeSubmoduleGitAction(repositoryPath, path).bind()
            updateSubmoduleGitAction(repositoryPath, path)
        }
    }
}