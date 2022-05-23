package app.viewmodels

import app.extensions.delayedStateChange
import app.git.DiffManager
import app.git.RefreshType
import app.git.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 300L

class CommitChangesViewModel @Inject constructor(
    private val tabState: TabState,
    private val diffManager: DiffManager,
) {
    private val _commitChangesStatus = MutableStateFlow<CommitChangesStatus>(CommitChangesStatus.Loading)
    val commitChangesStatus: StateFlow<CommitChangesStatus> = _commitChangesStatus

    fun loadChanges(commit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        delayedStateChange(
            delayMs = MIN_TIME_IN_MS_TO_SHOW_LOAD,
            onDelayTriggered = { _commitChangesStatus.value = CommitChangesStatus.Loading }
        ) {
            val changes = diffManager.commitDiffEntries(git, commit)

            _commitChangesStatus.value = CommitChangesStatus.Loaded(commit, changes)
        }
    }
}

sealed class CommitChangesStatus {
    object Loading : CommitChangesStatus()
    data class Loaded(val commit: RevCommit, val changes: List<DiffEntry>) : CommitChangesStatus()
}

