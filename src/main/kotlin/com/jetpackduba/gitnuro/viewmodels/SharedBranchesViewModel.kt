package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.CheckoutRefUseCase
import com.jetpackduba.gitnuro.git.branches.DeleteBranchUseCase
import com.jetpackduba.gitnuro.git.branches.MergeBranchUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseBranchUseCase
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.models.warningNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Ref
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Inject

interface ISharedBranchesViewModel {
    fun mergeBranch(ref: Ref): Job
    fun deleteBranch(branch: Ref): Job
    fun checkoutRef(ref: Ref): Job
    fun rebaseBranch(ref: Ref): Job
    fun copyBranchNameToClipboard(branch: Ref): Job
}

class SharedBranchesViewModel @Inject constructor(
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val tabState: TabState,
    private val appSettingsRepository: AppSettingsRepository,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
    private val clipboardManager: ClipboardManager
) : ISharedBranchesViewModel {

    override fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch merge",
        subtitle = "Merging branch ${ref.simpleName}",
        taskType = TaskType.MERGE_BRANCH,
        refreshEvenIfCrashes = true,
    ) { git ->
        if (mergeBranchUseCase(git, ref, appSettingsRepository.ffMerge)) {
            warningNotification("Merge produced conflicts, please fix them to continue.")
        } else {
            positiveNotification("Merged from \"${ref.simpleName}\"")
        }
    }

    override fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch delete",
        subtitle = "Deleting branch ${branch.simpleName}",
        taskType = TaskType.DELETE_BRANCH,
    ) { git ->
        deleteBranchUseCase(git, branch)

        positiveNotification("\"${branch.simpleName}\" deleted")
    }

    override fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch checkout",
        subtitle = "Checking out branch ${ref.simpleName}",
        taskType = TaskType.CHECKOUT_BRANCH,
    ) { git ->
        checkoutRefUseCase(git, ref)

        positiveNotification("\"${ref.simpleName}\" checked out")
    }

    override fun rebaseBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch rebase",
        subtitle = "Rebasing branch ${ref.simpleName}",
        taskType = TaskType.REBASE_BRANCH,
        refreshEvenIfCrashes = true,
    ) { git ->
        if (rebaseBranchUseCase(git, ref)) {
            warningNotification("Rebase produced conflicts, please fix them to continue.")
        } else {
            positiveNotification("\"${ref.simpleName}\" rebased")
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
