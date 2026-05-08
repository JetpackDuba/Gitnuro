package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class GetCommitFromRebaseLineUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val getCommitFromRebaseGitAction: IGetCommitFromRebaseLineGitAction,
) {
    suspend operator fun invoke(commit: Commit, shortMessage: String) {
        useCaseExecutor.execute(
            taskType = TaskType.GetCommitForRebase,
            onRefresh = {}
        ) { repositoryPath ->
            getCommitFromRebaseGitAction(repositoryPath, commit.hash, shortMessage)
        }

    }
}