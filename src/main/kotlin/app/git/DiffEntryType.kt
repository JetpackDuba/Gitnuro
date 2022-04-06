package app.git

import org.eclipse.jgit.diff.DiffEntry

sealed class DiffEntryType() {
    class CommitDiff(val diffEntry: DiffEntry) : DiffEntryType()

    sealed class UncommitedDiff(val statusEntry: StatusEntry) : DiffEntryType()

    sealed class UnstagedDiff(statusEntry: StatusEntry) : UncommitedDiff(statusEntry)
    sealed class StagedDiff(statusEntry: StatusEntry) : UncommitedDiff(statusEntry)

    /**
     * State used to represent staged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeStagedDiff(statusEntry: StatusEntry) : StagedDiff(statusEntry)
    /**
     * State used to represent unstaged changes when the repository state is not [org.eclipse.jgit.lib.RepositoryState.SAFE]
     */
    class UnsafeUnstagedDiff(statusEntry: StatusEntry) : UnstagedDiff(statusEntry)

    class SafeStagedDiff(statusEntry: StatusEntry) : StagedDiff(statusEntry)
    class SafeUnstagedDiff(statusEntry: StatusEntry) : UnstagedDiff(statusEntry)

}
