package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutRefGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPullFromSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPushToSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import kotlinx.coroutines.Job
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Inject

interface ISharedRemotesViewModel {
    fun deleteRemoteBranch(ref: Branch): Job
    fun checkoutRemoteBranch(remoteBranch: Branch): Job
    fun pushToRemoteBranch(branch: Branch): Job
    fun pullFromRemoteBranch(branch: Branch): Job
    fun copyBranchNameToClipboard(branch: Branch): Job
}

class SharedRemotesViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val deleteRemoteBranchGitAction: IDeleteRemoteBranchGitAction,
    private val checkoutRefGitAction: ICheckoutRefGitAction,
    private val pushToSpecificBranchGitAction: IPushToSpecificBranchGitAction,
    private val pullFromSpecificBranchGitAction: IPullFromSpecificBranchGitAction,
    private val clipboardManager: ClipboardManager,
) : ISharedRemotesViewModel {

    override fun deleteRemoteBranch(ref: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Deleting remote branch",
        subtitle = "Remote branch ${ref.simpleName} will be deleted from the remote",
        taskType = TaskType.DeleteRemoteBranch,
    ) { git ->
        deleteRemoteBranchGitAction(git.repository.directory.absolutePath, ref)

        positiveNotification("Remote branch \"${ref.simpleName}\" deleted")
    }

    override fun checkoutRemoteBranch(remoteBranch: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.CheckoutRemoteBranch,
    ) { git ->
        checkoutRefGitAction(git, remoteBranch)

        positiveNotification("\"${remoteBranch.simpleName}\" checked out")
    }

    override fun pushToRemoteBranch(branch: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Push",
        subtitle = "Pushing current branch to ${branch.simpleName}",
        taskType = TaskType.PushToBranch,
    ) { git ->
        pushToSpecificBranchGitAction(
            git.repository.directory.absolutePath,
            force = false,
            pushTags = false,
            remoteBranch = branch,
        )

        positiveNotification("Pushed to \"${branch.simpleName}\"")
    }

    override fun pullFromRemoteBranch(branch: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Pull",
        subtitle = "Pulling changes from ${branch.simpleName} to the current branch",
        taskType = TaskType.PullFromBranch,
    ) { git ->
        if (pullFromSpecificBranchGitAction(
                git.repository.directory.absolutePath,
                remoteBranch = branch,
                pullWithRebase = true /*TODO Fix once moved to use cases*/
            ).okOrNull()!!
        ) {
            warningNotification("Pull produced conflicts, fix them to continue")
        } else {
            positiveNotification("Pulled from \"${branch.simpleName}\"")
        }
    }

    override fun copyBranchNameToClipboard(branch: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
        taskType = TaskType.Unspecified
    ) {
        copyBranchNameToClipboardAndGetNotification(
            branch,
            clipboardManager
        )
    }
}
