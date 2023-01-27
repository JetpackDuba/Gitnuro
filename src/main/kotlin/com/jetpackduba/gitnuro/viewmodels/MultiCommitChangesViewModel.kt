package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.diff.GetCommitDiffEntriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.DepthWalk.Commit
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 300L

class MultiCommitChangesViewModel @Inject constructor(
    private val tabState: TabState,
    private val getCommitDiffEntriesUseCase: GetCommitDiffEntriesUseCase,
) {
    private val _commitsChangesStatus = MutableStateFlow<MultiCommitChangesStatus>(MultiCommitChangesStatus.Loading)
    val commitsChangesStatus: StateFlow<MultiCommitChangesStatus> = _commitsChangesStatus

    fun loadChanges(commits: List<RevCommit>) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        delayedStateChange(
            delayMs = MIN_TIME_IN_MS_TO_SHOW_LOAD,
            onDelayTriggered = { _commitsChangesStatus.value = MultiCommitChangesStatus.Loading }
        ) {
            val changes = commits
                .map { commit ->
                    CommitChanges(
                        commit = commit,
                        changes = getCommitDiffEntriesUseCase(git, commit)
                    )
                }

            _commitsChangesStatus.value = MultiCommitChangesStatus.Loaded(changes)
        }
    }
}

sealed class MultiCommitChangesStatus {
    object Loading : MultiCommitChangesStatus()
    data class Loaded(val changesList: List<CommitChanges>) : MultiCommitChangesStatus()
}

data class CommitChanges(
    val commit: RevCommit,
    val changes: List<DiffEntry>,
)