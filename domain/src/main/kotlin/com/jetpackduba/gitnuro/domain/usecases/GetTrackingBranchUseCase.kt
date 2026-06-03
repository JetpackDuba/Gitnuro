package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IGetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import javax.inject.Inject

class GetTrackingBranchUseCase @Inject constructor(
    private val getTrackingBranchGitAction: IGetTrackingBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(branch: Branch) = useCaseExecutor.execute { repositoryPath ->
        getTrackingBranchGitAction(repositoryPath, branch)
    }
}