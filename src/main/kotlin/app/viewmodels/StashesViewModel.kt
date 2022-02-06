package app.viewmodels

import app.git.RefreshType
import app.git.StashManager
import app.git.TabState
import app.ui.SelectedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class StashesViewModel @Inject constructor(
    private val stashManager: StashManager,
    private val tabState: TabState,
) {
    private val _stashStatus = MutableStateFlow<StashStatus>(StashStatus.Loaded(listOf()))
    val stashStatus: StateFlow<StashStatus>
        get() = _stashStatus

    suspend fun loadStashes(git: Git) {
        _stashStatus.value = StashStatus.Loading
        val stashList = stashManager.getStashList(git)
        _stashStatus.value = StashStatus.Loaded(stashList.toList()) // TODO: Is the list cast necessary?
    }

    suspend fun refresh(git: Git) {
        loadStashes(git)
    }

    fun applyStash(stashInfo: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        stashManager.applyStash(git, stashInfo)
    }

    fun popStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        stashManager.popStash(git, stash)

        stashDropped(stash)
    }

    fun deleteStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.STASHES,
    ) { git ->
        stashManager.deleteStash(git, stash)
        stashDropped(stash)
    }

    fun selectTab(stash: RevCommit) {
        tabState.newSelectedStash(stash)
    }

    private fun stashDropped(stash: RevCommit) {
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