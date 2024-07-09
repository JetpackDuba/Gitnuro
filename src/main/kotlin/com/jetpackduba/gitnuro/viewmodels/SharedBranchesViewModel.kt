package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.CheckoutRefUseCase
import com.jetpackduba.gitnuro.git.branches.DeleteBranchUseCase
import com.jetpackduba.gitnuro.git.branches.MergeBranchUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseBranchUseCase
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Job
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

interface ISharedBranchesViewModel {
    fun mergeBranch(ref: Ref): Job
    fun deleteBranch(branch: Ref): Job
    fun checkoutRef(ref: Ref): Job
    fun rebaseBranch(ref: Ref): Job
}

class SharedBranchesViewModel @Inject constructor(
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val tabState: TabState,
    private val appSettingsRepository: AppSettingsRepository,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
) : ISharedBranchesViewModel {

    override fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch merge",
        subtitle = "Merging branch ${ref.simpleName}",
        taskType = TaskType.MERGE_BRANCH,
        positiveFeedbackText = "Merged from \"${ref.simpleName}\"",
    ) { git ->
        mergeBranchUseCase(git, ref, appSettingsRepository.ffMerge)
    }

    override fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch delete",
        subtitle = "Deleting branch ${branch.simpleName}",
        taskType = TaskType.DELETE_BRANCH,
        positiveFeedbackText = "\"${branch.simpleName}\" deleted",
    ) { git ->
        deleteBranchUseCase(git, branch)
    }

    override fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch checkout",
        subtitle = "Checking out branch ${ref.simpleName}",
        taskType = TaskType.CHECKOUT_BRANCH,
        positiveFeedbackText = "\"${ref.simpleName}\" checked out",
    ) { git ->
        checkoutRefUseCase(git, ref)
    }

    override fun rebaseBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch rebase",
        subtitle = "Rebasing branch ${ref.simpleName}",
        taskType = TaskType.REBASE_BRANCH,
        positiveFeedbackText = "\"${ref.simpleName}\" rebased",
    ) { git ->
        rebaseBranchUseCase(git, ref)
    }
}
