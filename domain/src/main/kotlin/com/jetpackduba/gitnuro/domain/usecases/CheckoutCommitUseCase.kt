package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class CheckoutCommitUseCase @Inject constructor(
    private val checkoutCommitGitAction: ICheckoutCommitGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(commit: Commit) {
        invoke(commit.hash)
    }

    operator fun invoke(hash: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CheckoutCommit,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            checkoutCommitGitAction(repositoryPath, hash)
        }
    }
}