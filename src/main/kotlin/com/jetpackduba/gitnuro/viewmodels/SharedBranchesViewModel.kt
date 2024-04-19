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
import com.jetpackduba.gitnuro.repositories.BranchesVisibilityRepository
import kotlinx.coroutines.Job
import org.eclipse.jgit.internal.diffmergetool.MergeTools
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

interface ISharedBranchesViewModel {
    fun mergeBranch(ref: Ref): Job
    fun deleteBranch(branch: Ref): Job
    fun checkoutRef(ref: Ref): Job
    fun rebaseBranch(ref: Ref): Job
    fun alternateBranchVisibility(branch: Ref, isHidden: Boolean): Job
    fun hideAllBranches(branches: List<Ref>): Job
    fun showAllBranches(): Job
}

class SharedBranchesViewModel @Inject constructor(
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val tabState: TabState,
    private val appSettingsRepository: AppSettingsRepository,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
    private val branchesVisibilityRepository: BranchesVisibilityRepository,
) : ISharedBranchesViewModel {

    override fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch merge",
        subtitle = "Merging branch ${ref.simpleName}",
        taskType = TaskType.MERGE_BRANCH,
    ) { git ->
        mergeBranchUseCase(git, ref, appSettingsRepository.ffMerge)
    }

    override fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch delete",
        subtitle = "Deleting branch ${branch.simpleName}",
        taskType = TaskType.DELETE_BRANCH,
    ) { git ->
        deleteBranchUseCase(git, branch)
    }

    override fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch checkout",
        subtitle = "Checking out branch ${ref.simpleName}",
        taskType = TaskType.CHECKOUT_BRANCH,
    ) { git ->
        checkoutRefUseCase(git, ref)
    }

    override fun rebaseBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch rebase",
        subtitle = "Rebasing branch ${ref.simpleName}",
        taskType = TaskType.REBASE_BRANCH,
    ) { git ->
        rebaseBranchUseCase(git, ref)
    }

    override fun alternateBranchVisibility(branch: Ref, isHidden: Boolean) =
        tabState.runOperation(refreshType = RefreshType.NONE) { _ ->

            if (isHidden) {
                showBranch(branch)
            } else {
                hideBranch(branch)
            }
        }

    override fun hideAllBranches(branches: List<Ref>) = tabState.runOperation(refreshType = RefreshType.NONE) {
        branchesVisibilityRepository.hideBranches(branches.map { it.name })
    }

    override fun showAllBranches() = tabState.runOperation(refreshType = RefreshType.NONE) {
        branchesVisibilityRepository.showAllBranches()
    }

    private suspend fun hideBranch(ref: Ref) {
        branchesVisibilityRepository.hideBranches(listOf(ref.name))
    }

    private suspend fun showBranch(ref: Ref) = tabState.runOperation(refreshType = RefreshType.NONE) { _ ->
        branchesVisibilityRepository.showBranches(listOf(ref.name))
    }
}
