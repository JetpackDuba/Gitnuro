package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.git.branches.CreateBranchGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CreateBranchUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    val createBranchGitAction: CreateBranchGitAction,
){
    operator fun invoke(branchName: String, target: RevCommit?) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashesInteractive = { it is CheckoutConflictException },
        taskType = TaskType.CREATE_BRANCH,
    ) { git ->
        createBranchGitAction(git, branchName, target)

        positiveNotification("Branch \"${branchName}\" created")
    }
}