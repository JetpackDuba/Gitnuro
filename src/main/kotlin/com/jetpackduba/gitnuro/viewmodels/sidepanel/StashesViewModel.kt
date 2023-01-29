package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.stash.ApplyStashUseCase
import com.jetpackduba.gitnuro.git.stash.DeleteStashUseCase
import com.jetpackduba.gitnuro.git.stash.GetStashListUseCase
import com.jetpackduba.gitnuro.git.stash.PopStashUseCase
import com.jetpackduba.gitnuro.ui.SelectedItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

class StashesViewModel @AssistedInject constructor(
    private val getStashListUseCase: GetStashListUseCase,
    private val applyStashUseCase: ApplyStashUseCase,
    private val popStashUseCase: PopStashUseCase,
    private val deleteStashUseCase: DeleteStashUseCase,
    private val tabState: TabState,
    private val tabScope: CoroutineScope,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(true) {
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
                RefreshType.UNCOMMITED_CHANGES_AND_LOG
            ) {
                refresh(tabState.git)
            }
        }
    }

    suspend fun loadStashes(git: Git) {
        val stashList = getStashListUseCase(git)
        stashes.value = stashList
    }

    suspend fun refresh(git: Git) {
        loadStashes(git)
    }

    fun applyStash(stashInfo: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
    ) { git ->
        applyStashUseCase(git, stashInfo)
    }

    fun popStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
    ) { git ->
        popStashUseCase(git, stash)

        stashDropped(stash)
    }

    fun deleteStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.STASHES,
    ) { git ->
        deleteStashUseCase(git, stash)

        stashDropped(stash)
    }

    fun selectStash(stash: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        tabState.newSelectedStash(stash)
    }

    private fun stashDropped(stash: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        val selectedValue = tabState.selectedItem.value
        if (
            selectedValue is SelectedItem.Stash &&
            selectedValue.revCommit.name == stash.name
        ) {
            tabState.noneSelected()
        }
    }
}


data class StashesState(val stashes: List<RevCommit>, val isExpanded: Boolean)