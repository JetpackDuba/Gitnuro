package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.EntryContent
import org.eclipse.jgit.diff.DiffEntry

sealed class DiffResult(
    val diffEntry: DiffEntry,
) {
    class Text(
        diffEntry: DiffEntry,
        val hunks: List<Hunk>
    ) : DiffResult(diffEntry)

    class TextSplit(
        diffEntry: DiffEntry,
        val hunks: List<SplitHunk>
    ) : DiffResult(diffEntry)

    class NonText(
        diffEntry: DiffEntry,
        val oldBinaryContent: EntryContent,
        val newBinaryContent: EntryContent,
    ) : DiffResult(diffEntry)
}