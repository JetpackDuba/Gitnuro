package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.repositories.BranchesRepository
import com.jetpackduba.gitnuro.viewmodels.ISharedBranchesViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedBranchesViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.eclipse.jgit.lib.Ref

private const val TAG = "BranchesViewModel"


class BranchesViewModel @AssistedInject constructor(
    private val tabState: TabState,
    private val branchesRepository: BranchesRepository,
    tabScope: CoroutineScope,
    sharedBranchesViewModel: SharedBranchesViewModel,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(true), ISharedBranchesViewModel by sharedBranchesViewModel {
    private val branches = branchesRepository.branches
    private val currentBranch = branchesRepository.currentBranch

    val branchesState =
        combine(branches, currentBranch, isExpanded, filter) { branches, currentBranch, isExpanded, filter ->
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

    fun selectBranch(ref: Ref) {
        tabState.newSelectedRef(ref, ref.objectId)
    }
}

data class BranchesState(
    val branches: List<Ref>,
    val isExpanded: Boolean,
    val currentBranch: Ref?,
)