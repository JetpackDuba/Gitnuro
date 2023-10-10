package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.EntryContent
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.submodule.SubmoduleStatus

sealed class DiffResult(
    val diffEntry: DiffEntry,
) {
    sealed class TextDiff(diffEntry: DiffEntry): DiffResult(diffEntry)

    class Text(
        diffEntry: DiffEntry,
        val hunks: List<Hunk>
    ) : TextDiff(diffEntry)

    class TextSplit(
        diffEntry: DiffEntry,
        val hunks: List<SplitHunk>
    ) : TextDiff(diffEntry)

    class NonText(
        diffEntry: DiffEntry,
        val oldBinaryContent: EntryContent,
        val newBinaryContent: EntryContent,
    ) : DiffResult(diffEntry)

    class Submodule(
        diffEntry: DiffEntry,
        val submoduleStatus: SubmoduleStatus?,
    ) : DiffResult(diffEntry)
}