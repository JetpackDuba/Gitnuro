package app.viewmodels

import app.git.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class BranchesViewModel @Inject constructor(
    private val branchesManager: BranchesManager,
    private val rebaseManager: RebaseManager,
    private val mergeManager: MergeManager,
    private val tabState: TabState,
) {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches: StateFlow<List<Ref>>
        get() = _branches

    private val _currentBranch = MutableStateFlow<String>("")
    val currentBranch: StateFlow<String>
        get() = _currentBranch

    suspend fun loadBranches(git: Git) {
        _currentBranch.value = branchesManager.currentBranchRef(git)?.name ?: ""

        val branchesList = branchesManager.getBranches(git)

        // set selected branch as the first one always
        val selectedBranch = branchesList.find { it.name == _currentBranch.value }
        if (selectedBranch != null) {
            branchesList.remove(selectedBranch)
            branchesList.add(0, selectedBranch)
        }


        _branches.value = branchesList
    }

    fun createBranch(branchName: String) = tabState.safeProcessing { git ->
        branchesManager.createBranch(git, branchName)
        this.loadBranches(git)

        return@safeProcessing RefreshType.NONE
    }

    fun mergeBranch(ref: Ref, fastForward: Boolean) = tabState.safeProcessing { git ->
        mergeManager.mergeBranch(git, ref, fastForward)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun deleteBranch(branch: Ref) = tabState.safeProcessing { git ->
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

    fun rebaseBranch(ref: Ref) = tabState.safeProcessing { git ->
        rebaseManager.rebaseBranch(git, ref)

        return@safeProcessing RefreshType.ALL_DATA
    }
}