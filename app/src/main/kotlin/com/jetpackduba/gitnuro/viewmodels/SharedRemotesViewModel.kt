package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.extensions.simpleName
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutRefGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPullFromSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPushToSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import kotlinx.coroutines.Job
import org.eclipse.jgit.lib.Ref
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Inject

interface ISharedRemotesViewModel {
    fun deleteRemoteBranch(ref: Ref): Job
    fun checkoutRemoteBranch(remoteBranch: Ref): Job
    fun pushToRemoteBranch(branch: Ref): Job
    fun pullFromRemoteBranch(branch: Ref): Job
    fun copyBranchNameToClipboard(branch: Ref): Job
}

class SharedRemotesViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val deleteRemoteBranchGitAction: IDeleteRemoteBranchGitAction,
    private val checkoutRefGitAction: ICheckoutRefGitAction,
    private val pushToSpecificBranchGitAction: IPushToSpecificBranchGitAction,
    private val pullFromSpecificBranchGitAction: IPullFromSpecificBranchGitAction,
    private val clipboardManager: ClipboardManager,
) : ISharedRemotesViewModel {

    override fun deleteRemoteBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Deleting remote branch",
        subtitle = "Remote branch ${ref.simpleName} will be deleted from the remote",
        taskType = TaskType.DELETE_REMOTE_BRANCH,
    ) { git ->
        deleteRemoteBranchGitAction(git, ref)

        positiveNotification("Remote branch \"${ref.simpleName}\" deleted")
    }

    override fun checkoutRemoteBranch(remoteBranch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.CHECKOUT_REMOTE_BRANCH,
    ) { git ->
        checkoutRefGitAction(git, remoteBranch)

        positiveNotification("\"${remoteBranch.simpleName}\" checked out")
    }

    override fun pushToRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Push",
        subtitle = "Pushing current branch to ${branch.simpleName}",
        taskType = TaskType.PUSH_TO_BRANCH,
    ) { git ->
        pushToSpecificBranchGitAction(
            git = git,
            force = false,
            pushTags = false,
            remoteBranch = branch,
        )

        positiveNotification("Pushed to \"${branch.simpleName}\"")
    }

    override fun pullFromRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Pull",
        subtitle = "Pulling changes from ${branch.simpleName} to the current branch",
        taskType = TaskType.PULL_FROM_BRANCH,
    ) { git ->
        if (pullFromSpecificBranchGitAction(git = git, remoteBranch = branch, pullWithRebase = true /*TODO Fix once moved to use cases*/)) {
            warningNotification("Pull produced conflicts, fix them to continue")
        } else {
            positiveNotification("Pulled from \"${branch.simpleName}\"")
        }
    }

    override fun copyBranchNameToClipboard(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
        taskType = TaskType.UNSPECIFIED
    ) {
        copyBranchNameToClipboardAndGetNotification(
            branch,
            clipboardManager
        )
    }
}
