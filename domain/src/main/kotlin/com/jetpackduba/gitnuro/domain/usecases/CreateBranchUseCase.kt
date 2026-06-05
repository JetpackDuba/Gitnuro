package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.ICreateBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class CreateBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    val createBranchGitAction: ICreateBranchGitAction,
    val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(branchName: String, target: Commit?) {
        // TODO Should be "execute" and handle the result in the UI
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CreateBranch,
            refreshEvenIfFailed = true, // TODO Previously this was a conditional lambda: refreshEvenIfCrashesInteractive = { it is CheckoutConflictException },
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            createBranchGitAction(repositoryPath, branchName, target)
        }
    }
}