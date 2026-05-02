package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IRebaseBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import javax.inject.Inject

class RebaseBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val rebaseBranchGitAction: IRebaseBranchGitAction,
) {
    operator fun invoke(branch: Branch) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.RebaseBranch,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            rebaseBranchGitAction(repositoryPath, branch)
        }
    }
}