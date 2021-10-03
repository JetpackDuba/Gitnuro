package git

import org.eclipse.jgit.diff.DiffEntry

sealed class DiffEntryType(val diffEntry: DiffEntry) {
    class CommitDiff(diffEntry: DiffEntry): DiffEntryType(diffEntry)
    class UnstagedDiff(diffEntry: DiffEntry): DiffEntryType(diffEntry)
    class StagedDiff(diffEntry: DiffEntry): DiffEntryType(diffEntry)
}
