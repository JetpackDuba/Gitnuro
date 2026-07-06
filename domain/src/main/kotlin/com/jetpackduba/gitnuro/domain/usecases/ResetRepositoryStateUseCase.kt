package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IAbortRebaseGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IResetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class ResetRepositoryStateUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val resetRepositoryStateGitAction: IResetRepositoryStateGitAction,
) {
    operator fun invoke() {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ResetRepoState,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            resetRepositoryStateGitAction(repositoryPath)
        }
    }
}
