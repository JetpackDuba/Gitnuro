package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.extensions.simpleName
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
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
    private val tabState: TabInstanceRepository,
    private val repositoryDataRepository: RepositoryDataRepository,
    tabScope: CoroutineScope,
    sharedBranchesViewModel: SharedBranchesViewModel,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(true), ISharedBranchesViewModel by sharedBranchesViewModel {
    private val branches = repositoryDataRepository.localBranches
    private val currentBranch = repositoryDataRepository.currentBranch

    val branchesState =
        combine(branches, currentBranch, isExpanded, filter) { branches, currentBranch, isExpanded, filter ->
            BranchesState(
                branches = branches.filter { it.name.lowercaseContains(filter) },
                isExpanded = isExpanded,
                currentBranch = currentBranch
            )
        }.stateIn(
            scope = tabScope,
            started = SharingStarted.Eagerly,
            initialValue = BranchesState(emptyList(), isExpanded.value, null)
        )

    fun selectBranch(ref: Branch) {
        tabState.newSelectedRef(ref, ref.hash)
    }
}

data class BranchesState(
    val branches: List<Branch>,
    val isExpanded: Boolean,
    val currentBranch: Branch?,
)