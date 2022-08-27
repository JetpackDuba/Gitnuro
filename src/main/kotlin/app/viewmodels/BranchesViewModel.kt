package app.viewmodels

import app.git.*
import app.git.branches.*
import app.git.remote_operations.PullFromSpecificBranchUseCase
import app.git.remote_operations.PushToSpecificBranchUseCase
import app.preferences.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class BranchesViewModel @Inject constructor(
    private val rebaseManager: RebaseManager,
    private val tabState: TabState,
    private val appSettings: AppSettings,
    private val pushToSpecificBranchUseCase: PushToSpecificBranchUseCase,
    private val pullFromSpecificBranchUseCase: PullFromSpecificBranchUseCase,
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val createBranchUseCase: CreateBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
) : ExpandableViewModel(true) {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches: StateFlow<List<Ref>>
        get() = _branches

    private val _currentBranch = MutableStateFlow<Ref?>(null)
    val currentBranch: StateFlow<Ref?>
        get() = _currentBranch

    private suspend fun loadBranches(git: Git) {
        _currentBranch.value = getCurrentBranchUseCase(git)

        val branchesList = getBranchesUseCase(git).toMutableList()

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
        createBranchUseCase(git, branchName)
        this.loadBranches(git)
    }

    fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        mergeBranchUseCase(git, ref, appSettings.ffMerge)
    }

    fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        deleteBranchUseCase(git, branch)
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        checkoutRefUseCase(git, ref)
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
        pushToSpecificBranchUseCase(
            git = git,
            force = false,
            pushTags = false,
            remoteBranch = branch,
        )
    }

    fun pullFromRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        pullFromSpecificBranchUseCase(
            git = git,
            rebase = false,
            remoteBranch = branch,
        )
    }
}