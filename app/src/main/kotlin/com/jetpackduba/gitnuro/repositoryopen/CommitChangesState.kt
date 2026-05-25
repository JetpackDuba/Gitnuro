package com.jetpackduba.gitnuro.repositoryopen

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.diff.DiffEntry

private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 300L


sealed interface CommitChangesState {
    data object Loading : CommitChangesState
    data class Loaded(val commit: Commit, val changes: List<DiffEntry>) :
        CommitChangesState
}

