package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.*
import com.jetpackduba.gitnuro.viewmodels.ISharedBranchesViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedBranchesViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

private const val TAG = "BranchesViewModel"


class BranchesViewModel @AssistedInject constructor(
    private val tabState: TabState,
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    tabScope: CoroutineScope,
    sharedBranchesViewModel: SharedBranchesViewModel,
    @Assisted
    private val filter: StateFlow<String>
) : SidePanelChildViewModel(true), ISharedBranchesViewModel by sharedBranchesViewModel {
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
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA) {
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

    suspend fun refresh(git: Git) {
        loadBranches(git)
    }

    fun selectBranch(ref: Ref) {
        tabState.newSelectedRef(ref, ref.objectId)
    }
}

data class BranchesState(
    val branches: List<Ref>,
    val isExpanded: Boolean,
    val currentBranch: Ref?,
)