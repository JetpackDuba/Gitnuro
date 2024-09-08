package com.jetpackduba.gitnuro.viewmodels.sidepanel

import androidx.compose.runtime.collectAsState
import com.jetpackduba.gitnuro.di.factories.*
import com.jetpackduba.gitnuro.git.CloseableView
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.ui.SelectedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class SidePanelViewModel @Inject constructor(
    branchesViewModelFactory: BranchesViewModelFactory,
    remotesViewModelFactory: RemotesViewModelFactory,
    tagsViewModelFactory: TagsViewModelFactory,
    stashesViewModelFactory: StashesViewModelFactory,
    submodulesViewModelFactory: SubmodulesViewModelFactory,
    private val tabState: TabState,
    private val tabScope: CoroutineScope,
) {
    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    val branchesViewModel: BranchesViewModel = branchesViewModelFactory.create(filter)
    val remotesViewModel: RemotesViewModel = remotesViewModelFactory.create(filter)
    val tagsViewModel: TagsViewModel = tagsViewModelFactory.create(filter)
    val stashesViewModel: StashesViewModel = stashesViewModelFactory.create(filter)
    val submodulesViewModel: SubmodulesViewModel = submodulesViewModelFactory.create(filter)

    private val _freeSearchFocusFlow = MutableSharedFlow<Unit>()
    val freeSearchFocusFlow = _freeSearchFocusFlow.asSharedFlow()

    init {
        tabScope.launch {
            tabState.closeViewFlow.collectLatest {
                if (it == CloseableView.SIDE_PANEL_SEARCH) {
                    newFilter("")
                    _freeSearchFocusFlow.emit(Unit)
                }
            }
        }
    }

    fun newFilter(newValue: String) {
        _filter.value = newValue
    }

    fun addSidePanelSearchToCloseables() = tabScope.launch {
        tabState.addCloseableView(CloseableView.SIDE_PANEL_SEARCH)
    }

    fun removeSidePanelSearchFromCloseables() = tabScope.launch {
        tabState.removeCloseableView(CloseableView.SIDE_PANEL_SEARCH)
    }
}
