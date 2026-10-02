package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

sealed class DiffSelected(val entries: Set<DiffType>) {
    @Immutable
    data class CommitedChanges(
        val items: Set<DiffType.CommitDiff>
    ) : DiffSelected(items)

    @Immutable
    data class UncommittedChanges(
        val entryType: EntryType,
        val items: Set<DiffType.UncommittedDiff>
    ) : DiffSelected(items)
}