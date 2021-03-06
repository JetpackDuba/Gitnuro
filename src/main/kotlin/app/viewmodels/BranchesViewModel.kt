package app.viewmodels

import app.git.*
import app.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class BranchesViewModel @Inject constructor(
    private val branchesManager: BranchesManager,
    private val rebaseManager: RebaseManager,
    private val mergeManager: MergeManager,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val tabState: TabState,
    private val appPreferences: AppPreferences,
) : ExpandableViewModel(true) {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches: StateFlow<List<Ref>>
        get() = _branches

    private val _currentBranch = MutableStateFlow<Ref?>(null)
    val currentBranch: StateFlow<Ref?>
        get() = _currentBranch

    suspend fun loadBranches(git: Git) {
        _currentBranch.value = branchesManager.currentBranchRef(git)

        val branchesList = branchesManager.getBranches(git)

        // set selected branch as the first one always
        val selectedBranch = branchesList.find { it.name == _currentBranch.value?.name }
        if (selectedBranch != null) {
            branchesList.remove(selectedBranch)
            branchesList.add(0, selectedBranch)
        }


        _branches.value = branchesList
    }

    fun createBranch(branchName: String) = tabState.safeProcessing(
        refreshType = RefreshType.ONLY_LOG,
        refreshEvenIfCrashes = true,
    ) { git ->
        branchesManager.createBranch(git, branchName)
        this.loadBranches(git)
    }

    fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        mergeManager.mergeBranch(git, ref, appPreferences.ffMerge)
    }

    fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.deleteBranch(git, branch)
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.checkoutRef(git, ref)
    }

    suspend fun refresh(git: Git) {
        loadBranches(git)
    }

    fun rebaseBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        rebaseManager.rebaseBranch(git, ref)
    }

    fun selectBranch(ref: Ref) {
        tabState.newSelectedRef(ref.objectId)
    }

    fun pushToRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        remoteOperationsManager.pushToBranch(
            git = git,
            force = false,
            pushTags = false,
            remoteBranch = branch,
        )
    }

    fun pullFromRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        remoteOperationsManager.pullFromBranch(
            git = git,
            rebase = false,
            remoteBranch = branch,
        )
    }
}