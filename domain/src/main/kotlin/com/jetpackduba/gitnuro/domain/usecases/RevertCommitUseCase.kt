package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IRevertCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class RevertCommitUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val revertCommitGitAction: IRevertCommitGitAction,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val refreshStatusUseCase: RefreshStatusUseCase,
) {
    operator fun invoke(commit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.RevertCommit,
            refreshEvenIfFailed = true,
            onRefresh = {
                refreshStatusUseCase()
                refreshLogUseCase()
            }
        ) { repositoryPath ->
            revertCommitGitAction(repositoryPath, commit)
        }
    }
}