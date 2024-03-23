package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.CheckoutRefUseCase
import com.jetpackduba.gitnuro.git.remote_operations.DeleteRemoteBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullFromSpecificBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PushToSpecificBranchUseCase
import kotlinx.coroutines.Job
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

interface ISharedRemotesViewModel {
    fun deleteRemoteBranch(ref: Ref): Job
    fun checkoutRemoteBranch(remoteBranch: Ref): Job
    fun pushToRemoteBranch(branch: Ref): Job
    fun pullFromRemoteBranch(branch: Ref): Job
}

class SharedRemotesViewModel @Inject constructor(
    private val tabState: TabState,
    private val deleteRemoteBranchUseCase: DeleteRemoteBranchUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
    private val pushToSpecificBranchUseCase: PushToSpecificBranchUseCase,
    private val pullFromSpecificBranchUseCase: PullFromSpecificBranchUseCase,
) : ISharedRemotesViewModel {
   override fun deleteRemoteBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Deleting remote branch",
        subtitle = "Remote branch ${ref.simpleName} will be deleted from the remote",
       taskType = TaskType.DELETE_REMOTE_BRANCH,
    ) { git ->
        deleteRemoteBranchUseCase(git, ref)
    }

    override fun checkoutRemoteBranch(remoteBranch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.CHECKOUT_REMOTE_BRANCH,
    ) { git ->
        checkoutRefUseCase(git, remoteBranch)
    }

    override fun pushToRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Push",
        subtitle = "Pushing current branch to ${branch.simpleName}",
        taskType = TaskType.PUSH_TO_BRANCH,
    ) { git ->
        pushToSpecificBranchUseCase(
            git = git,
            force = false,
            pushTags = false,
            remoteBranch = branch,
        )
    }

    override fun pullFromRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Pull",
        subtitle = "Pulling changes from ${branch.simpleName} to the current branch",
        taskType = TaskType.PULL_FROM_BRANCH,
    ) { git ->
        pullFromSpecificBranchUseCase(
            git = git,
            rebase = false,
            remoteBranch = branch,
        )
    }
}
