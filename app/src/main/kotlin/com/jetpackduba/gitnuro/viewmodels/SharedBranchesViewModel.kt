package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutRefGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IRebaseBranchGitAction
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.usecases.DeleteBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.MergeBranchUseCase
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import kotlinx.coroutines.Job
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Inject

interface ISharedBranchesViewModel {
    fun mergeBranch(branch: Branch)
    fun deleteBranch(branch: Branch)
    fun checkoutBranch(ref: Branch): Job
    fun rebaseBranch(ref: Branch): Job
    fun copyBranchNameToClipboard(branch: Branch): Job
}

class SharedBranchesViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val rebaseBranchGitAction: IRebaseBranchGitAction,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val checkoutRefGitAction: ICheckoutRefGitAction,
    private val clipboardManager: ClipboardManager,
) : ISharedBranchesViewModel {

    override fun mergeBranch(branch: Branch) = mergeBranchUseCase(branch)

    override fun deleteBranch(branch: Branch) = deleteBranchUseCase(branch)

    override fun checkoutBranch(ref: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch checkout",
        subtitle = "Checking out branch ${ref.simpleName}",
        taskType = TaskType.CheckoutBranch,
    ) { git ->
        checkoutRefGitAction(git, ref)

        positiveNotification("\"${ref.simpleName}\" checked out")
    }

    override fun rebaseBranch(ref: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch rebase",
        subtitle = "Rebasing branch ${ref.simpleName}",
        taskType = TaskType.RebaseBranch,
        refreshEvenIfCrashes = true,
    ) { git ->
        if (rebaseBranchGitAction(git, ref)) {
            warningNotification("Rebase produced conflicts, please fix them to continue.")
        } else {
            positiveNotification("\"${ref.simpleName}\" rebased")
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
