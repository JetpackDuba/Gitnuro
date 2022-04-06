package app.git

import org.eclipse.jgit.diff.DiffEntry

sealed class DiffEntryType(val statusEntry: StatusEntry) {
    class CommitDiff(diffEntry: DiffEntry) : DiffEntryType(diffEntry)

    sealed class UnstagedDiff(diffEntry: DiffEntry) : DiffEntryType(diffEntry)
    sealed class StagedDiff(diffEntry: DiffEntry) : DiffEntryType(diffEntry)

    /**
     * State used to represent staged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeStagedDiff(diffEntry: DiffEntry) : StagedDiff(diffEntry)
    /**
     * State used to represent unstaged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeUnstagedDiff(diffEntry: DiffEntry) : UnstagedDiff(diffEntry)

    class SafeStagedDiff(diffEntry: DiffEntry) : StagedDiff(diffEntry)
    class SafeUnstagedDiff(diffEntry: DiffEntry) : UnstagedDiff(diffEntry)

}
