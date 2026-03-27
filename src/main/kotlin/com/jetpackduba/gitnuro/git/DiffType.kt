package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.extensions.toStatusType
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import org.eclipse.jgit.diff.DiffEntry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
sealed class DiffType {
    abstract val filePath: String
    abstract val statusType: StatusType

    data class CommitDiff(
        val diffEntry: DiffEntry,
        val commitsCount: Int = 1,
    ) : DiffType() {
        override val filePath: String
            get() = diffEntry.filePath

        override val statusType: StatusType
            get() = diffEntry.toStatusType()
    }

    data class UncommittedDiff(val statusEntry: StatusEntry, val entryType: EntryType) : DiffType() {
        override val filePath: String
            get() = statusEntry.filePath

        override val statusType: StatusType
            get() = statusEntry.statusType
    }

    val isStagedDiff: Boolean
        get() {
            contract { returns(true) implies (this@DiffType is UncommittedDiff) }
            return this is UncommittedDiff && this.entryType == EntryType.STAGED
        }

    val isUnstagedDiff: Boolean
        get() {
            contract { returns(true) implies (this@DiffType is UncommittedDiff) }
            return this is UncommittedDiff && this.entryType == EntryType.UNSTAGED
        }

}
