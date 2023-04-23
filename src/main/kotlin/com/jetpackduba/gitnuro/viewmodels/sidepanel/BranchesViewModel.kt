package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.*
import com.jetpackduba.gitnuro.git.rebase.RebaseBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullFromSpecificBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PushToSpecificBranchUseCase
import com.jetpackduba.gitnuro.preferences.AppSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

private const val TAG = "BranchesViewModel"


class BranchesViewModel @AssistedInject constructor(
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val tabState: TabState,
    private val appSettings: AppSettings,
    private val pushToSpecificBranchUseCase: PushToSpecificBranchUseCase,
    private val pullFromSpecificBranchUseCase: PullFromSpecificBranchUseCase,
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
    private val tabScope: CoroutineScope,
    @Assisted
    private val filter: StateFlow<String>
) : SidePanelChildViewModel(true) {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    private val _currentBranch = MutableStateFlow<Ref?>(null)

    val branchesState =
        combine(_branches, _currentBranch, isExpanded, filter) { branches, currentBranch, isExpanded, filter ->
            BranchesState(
                branches = branches.filter { it.simpleName.lowercaseContains(filter) },
                isExpanded = isExpanded,
                currentBranch = currentBranch
            )
        }.stateIn(
            scope = tabScope,
            started = SharingStarted.Eagerly,
            initialValue = BranchesState(emptyList(), isExpanded.value, null)
        )

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA)
            {
                refresh(tabState.git)
            }
        }
    }

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
        rebaseBranchUseCase(git, ref)
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

data class BranchesState(
    val branches: List<Ref>,
    val isExpanded: Boolean,
    val currentBranch: Ref?,
)