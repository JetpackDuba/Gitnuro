package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Stable

data class Status(
    val staged: List<StatusEntry>,
    val unstaged: List<StatusEntry>,
    val ignored: List<String>,
)


data class StatusEntry(val filePath: String, val statusType: StatusType, val entryType: EntryType = EntryType.UNSTAGED)

enum class StatusType {
    ADDED,
    MODIFIED,
    REMOVED,
    CONFLICTING,
}

@Stable
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