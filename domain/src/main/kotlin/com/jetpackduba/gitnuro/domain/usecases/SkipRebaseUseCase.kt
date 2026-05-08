package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IAbortRebaseGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISkipRebaseGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class SkipRebaseUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val skipRebaseGitAction: ISkipRebaseGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke() {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.SkipRebase,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            skipRebaseGitAction(repositoryPath)
        }
    }
}