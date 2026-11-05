package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

data class Status(
    val staged: List<StatusEntry> = emptyList(),
    val unstaged: List<StatusEntry> = emptyList(),
    val ignored: List<String> = emptyList(),
)


data class StatusEntry(
    val oldPath: String?,
    val newPath: String?,
    val statusType: StatusType,
    val entryType: EntryType = EntryType.UNSTAGED,
) {
    val filePath: String get() = newPath ?: oldPath ?: ""
}

enum class StatusType {
    ADDED,
    MODIFIED,
    REMOVED,
    CONFLICTING,
    RENAMED,
}

@Immutable
data class StatusSummary(
    val modifiedCount: Int,
    val deletedCount: Int,
    val addedCount: Int,
    val conflictingCount: Int,
) {
    val total = modifiedCount +
            deletedCount +
            addedCount +
            conflictingCount
}