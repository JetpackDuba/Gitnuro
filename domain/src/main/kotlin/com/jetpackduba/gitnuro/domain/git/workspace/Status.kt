package com.jetpackduba.gitnuro.domain.git.workspace

import com.jetpackduba.gitnuro.domain.git.EntryType

data class StatusEntry(val filePath: String, val statusType: StatusType, val entryType: EntryType = EntryType.UNSTAGED)

enum class StatusType {
    ADDED,
    MODIFIED,
    REMOVED,
    CONFLICTING,
}

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