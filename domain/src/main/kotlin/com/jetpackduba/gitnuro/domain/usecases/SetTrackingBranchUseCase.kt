package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IGetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class SetTrackingBranchUseCase @Inject constructor(
    private val setTrackingBranchGitAction: ISetTrackingBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(branch: Branch, remoteName: String?, remoteBranch: Branch?) = useCaseExecutor.executeLaunch(
        taskType = TaskType.ChangeBranchUpstream,
        dataToRefresh = arrayOf(DataToRefresh.LOG, DataToRefresh.BRANCHES),
    ) {repositoryPath ->
        setTrackingBranchGitAction(repositoryPath, branch, remoteName, remoteBranch)
    }
}