package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IStartRebaseInteractiveGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class StartRebaseInteractiveUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val startRebaseInteractiveGitAction: IStartRebaseInteractiveGitAction,
) {
    operator fun invoke(commit: Commit) =  useCaseExecutor.executeLaunch(
        taskType = TaskType.RebaseInteractive,
        onRefresh = {
            // TODO Refresh rebase interactive state?
        }
    ) { repositoryPath ->
        startRebaseInteractiveGitAction(repositoryPath, commit)
    }
}