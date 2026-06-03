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
    private val refreshLogUseCase: RefreshLogUseCase,
    private val refreshBranchesUseCase: RefreshBranchesUseCase,
) {
    operator fun invoke(branch: Branch, remoteName: String?, remoteBranch: Branch?) = useCaseExecutor.executeLaunch(
        taskType = TaskType.ChangeBranchUpstream,
        onRefresh = {
            refreshLogUseCase()
            refreshBranchesUseCase()
        }
    ) {repositoryPath ->
        setTrackingBranchGitAction(repositoryPath, branch, remoteName, remoteBranch)
    }
}