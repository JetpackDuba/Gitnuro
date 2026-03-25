package com.jetpackduba.gitnuro.domain.models

sealed class DiffSelected(val entries: Set<DiffType>) {
    data class CommitedChanges(
        val items: Set<DiffType.CommitDiff>
    ) : DiffSelected(items)

    data class UncommittedChanges(
        val entryType: EntryType,
        val items: Set<DiffType.UncommittedDiff>
    ) : DiffSelected(items)
}