package app.viewmodels

import app.git.BranchesManager
import app.git.RefreshType
import app.git.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class BranchesViewModel @Inject constructor(
    private val branchesManager: BranchesManager,
    private val tabState: TabState,
) {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches: StateFlow<List<Ref>>
        get() = _branches

    private val _currentBranch = MutableStateFlow<String>("")
    val currentBranch: StateFlow<String>
        get() = _currentBranch

    suspend fun loadBranches(git: Git) {
        val branchesList = branchesManager.getBranches(git)

        _branches.value = branchesList
        _currentBranch.value = branchesManager.currentBranchRef(git)?.name ?: ""
    }

    fun createBranch(branchName: String) = tabState.safeProcessing { git ->
        branchesManager.createBranch(git, branchName)
        this.loadBranches(git)

        return@safeProcessing RefreshType.NONE
    }

    fun mergeBranch(ref: Ref, fastForward: Boolean) = tabState.safeProcessing { git ->
        branchesManager.mergeBranch(git, ref, fastForward)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun deleteBranch(branch: Ref) =tabState.safeProcessing { git ->
        branchesManager.deleteBranch(git, branch)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing { git ->
        branchesManager.checkoutRef(git, ref)

        return@safeProcessing RefreshType.ALL_DATA
    }

    suspend fun refresh(git: Git) {
        loadBranches(git)
    }
}