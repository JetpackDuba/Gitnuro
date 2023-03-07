package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.di.factories.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SidePanelViewModel @Inject constructor(
    branchesViewModelFactory: BranchesViewModelFactory,
    remotesViewModelFactory: RemotesViewModelFactory,
    tagsViewModelFactory: TagsViewModelFactory,
    stashesViewModelFactory: StashesViewModelFactory,
    submodulesViewModelFactory: SubmodulesViewModelFactory,
) {
    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter

    val branchesViewModel: BranchesViewModel = branchesViewModelFactory.create(filter)
    val remotesViewModel: RemotesViewModel = remotesViewModelFactory.create(filter)
    val tagsViewModel: TagsViewModel = tagsViewModelFactory.create(filter)
    val stashesViewModel: StashesViewModel = stashesViewModelFactory.create(filter)
    val submodulesViewModel: SubmodulesViewModel = submodulesViewModelFactory.create(filter)

    fun newFilter(newValue: String) {
        _filter.value = newValue
    }
}
