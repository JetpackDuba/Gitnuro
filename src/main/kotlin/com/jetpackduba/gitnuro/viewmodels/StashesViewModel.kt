package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.stash.ApplyStashUseCase
import com.jetpackduba.gitnuro.git.stash.DeleteStashUseCase
import com.jetpackduba.gitnuro.git.stash.GetStashListUseCase
import com.jetpackduba.gitnuro.git.stash.PopStashUseCase
import com.jetpackduba.gitnuro.ui.SelectedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class StashesViewModel @Inject constructor(
    private val getStashListUseCase: GetStashListUseCase,
    private val applyStashUseCase: ApplyStashUseCase,
    private val popStashUseCase: PopStashUseCase,
    private val deleteStashUseCase: DeleteStashUseCase,
    private val tabState: TabState,
    private val tabScope: CoroutineScope,
) : ExpandableViewModel(true) {
    private val _stashStatus = MutableStateFlow<StashStatus>(StashStatus.Loaded(listOf()))

    val stashStatus: StateFlow<StashStatus>
        get() = _stashStatus

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
        _stashStatus.value = StashStatus.Loading
        val stashList = getStashListUseCase(git)
        _stashStatus.value = StashStatus.Loaded(stashList.toList())
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

    fun selectTab(stash: RevCommit) = tabState.runOperation(
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


sealed class StashStatus {
    object Loading : StashStatus()
    data class Loaded(val stashes: List<RevCommit>) : StashStatus()
}