package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.stash.GetStashListUseCase
import com.jetpackduba.gitnuro.viewmodels.ISharedStashViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedStashViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

class StashesViewModel @AssistedInject constructor(
    private val getStashListUseCase: GetStashListUseCase,
    private val tabState: TabState,
    private val tabScope: CoroutineScope,
    @Assisted
    private val filter: StateFlow<String>,
    sharedStashViewModel: SharedStashViewModel,
) : SidePanelChildViewModel(true), ISharedStashViewModel by sharedStashViewModel {

    private val stashes = MutableStateFlow<List<RevCommit>>(emptyList())

    val stashesState: StateFlow<StashesState> = combine(stashes, isExpanded, filter) { stashes, isExpanded, filter ->
        StashesState(
            stashes = stashes.filter { it.fullMessage.lowercaseContains(filter) },
            isExpanded,
        )
    }.stateIn(
        tabScope,
        SharingStarted.Eagerly,
        StashesState(emptyList(), isExpanded.value)
    )

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.ALL_DATA,
                RefreshType.STASHES,
                RefreshType.UNCOMMITTED_CHANGES_AND_LOG
            ) {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadStashes(git: Git) {
        val stashList = getStashListUseCase(git)
        stashes.value = stashList
    }

    suspend fun refresh(git: Git) {
        loadStashes(git)
    }
}


data class StashesState(val stashes: List<RevCommit>, val isExpanded: Boolean)