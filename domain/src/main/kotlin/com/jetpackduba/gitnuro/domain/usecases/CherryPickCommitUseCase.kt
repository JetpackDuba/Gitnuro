package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.ICherryPickCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class CherryPickCommitUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val cherryPickGitAction: ICherryPickCommitGitAction,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val refreshStatusUseCase: RefreshStatusUseCase,
) {
    operator fun invoke(commit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CherryPickCommit,
            onRefresh = {
                refreshStatusUseCase()
                refreshLogUseCase()
            }
        ) { repositoryPath ->
            cherryPickGitAction(repositoryPath, commit)
        }
    }
}