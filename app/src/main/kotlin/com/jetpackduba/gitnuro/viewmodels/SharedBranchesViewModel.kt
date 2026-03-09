package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.extensions.simpleName
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.git.branches.CheckoutRefGitAction
import com.jetpackduba.gitnuro.domain.git.branches.DeleteBranchGitAction
import com.jetpackduba.gitnuro.domain.git.branches.MergeBranchGitAction
import com.jetpackduba.gitnuro.domain.git.rebase.RebaseBranchGitAction
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreAppSettingsRepository
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
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
    private val rebaseBranchGitAction: RebaseBranchGitAction,
    private val tabState: TabInstanceRepository,
    private val appSettings: AppSettingsService,
    private val mergeBranchGitAction: MergeBranchGitAction,
    private val deleteBranchGitAction: DeleteBranchGitAction,
    private val checkoutRefGitAction: CheckoutRefGitAction,
    private val clipboardManager: ClipboardManager,
) : ISharedBranchesViewModel {

    override fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch merge",
        subtitle = "Merging branch ${ref.simpleName}",
        taskType = TaskType.MERGE_BRANCH,
        refreshEvenIfCrashes = true,
    ) { git ->
        if (mergeBranchGitAction(git, ref, appSettings.fastForwardMerge.first(), mergeAutoStash = true)) { // TODO fix after refactor
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
        deleteBranchGitAction(git, branch)

        positiveNotification("\"${branch.simpleName}\" deleted")
    }

    override fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch checkout",
        subtitle = "Checking out branch ${ref.simpleName}",
        taskType = TaskType.CHECKOUT_BRANCH,
    ) { git ->
        checkoutRefGitAction(git, ref)

        positiveNotification("\"${ref.simpleName}\" checked out")
    }

    override fun rebaseBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch rebase",
        subtitle = "Rebasing branch ${ref.simpleName}",
        taskType = TaskType.REBASE_BRANCH,
        refreshEvenIfCrashes = true,
    ) { git ->
        if (rebaseBranchGitAction(git, ref)) {
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
