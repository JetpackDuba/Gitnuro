package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IApplyStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class ApplyStashUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val applyStashGitAction: IApplyStashGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(stashCommit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ApplyStash,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            applyStashGitAction(repositoryPath, stashCommit)
        }
    }
}