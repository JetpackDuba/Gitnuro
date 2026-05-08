package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IAbortRebaseGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class AbortRebaseUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val abortRebaseGitAction: IAbortRebaseGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke() {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.AbortRebase,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            abortRebaseGitAction(repositoryPath)
        }
    }
}