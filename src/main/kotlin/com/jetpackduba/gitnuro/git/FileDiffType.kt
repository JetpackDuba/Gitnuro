package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.extensions.toStatusType
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import org.eclipse.jgit.diff.DiffEntry

sealed interface DiffType

class MultiFileDiffType(val values: List<FileDiffType>) : DiffType

sealed interface FileDiffType : DiffType {
    class CommitFileDiff(val diffEntry: DiffEntry) : FileDiffType {
        override val filePath: String
            get() = diffEntry.filePath

        override val statusType: StatusType
            get() = diffEntry.toStatusType()
    }

    sealed class UncommittedFileDiff(val statusEntry: StatusEntry) : FileDiffType {
        override val filePath: String
            get() = statusEntry.filePath

        override val statusType: StatusType
            get() = statusEntry.statusType
    }

    sealed class UnstagedFileDiff(statusEntry: StatusEntry) : UncommittedFileDiff(statusEntry)
    sealed class StagedFileDiff(statusEntry: StatusEntry) : UncommittedFileDiff(statusEntry)

    /**
     * State used to represent staged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeStagedFileDiff(statusEntry: StatusEntry) : StagedFileDiff(statusEntry)

    /**
     * State used to represent unstaged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeUnstagedFileDiff(statusEntry: StatusEntry) : UnstagedFileDiff(statusEntry)

    class SafeStagedFileDiff(statusEntry: StatusEntry) : StagedFileDiff(statusEntry)
    class SafeUnstagedFileDiff(statusEntry: StatusEntry) : UnstagedFileDiff(statusEntry)

    val filePath: String
    val statusType: StatusType

}
