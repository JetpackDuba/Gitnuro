package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.RawFileManager
import com.jetpackduba.gitnuro.git.diff.Hunk
import com.jetpackduba.gitnuro.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.nio.ByteBuffer
import javax.inject.Inject

class UnstageHunkUseCase @Inject constructor(
    private val rawFileManager: RawFileManager,
    private val getLinesFromRawTextUseCase: GetLinesFromRawTextUseCase,
) {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
        val repository = git.repository
        val dirCache = repository.lockDirCache()
        val dirCacheEditor = dirCache.editor()
        var completedWithErrors = true

        try {
            val entryContent = rawFileManager.getRawContent(
                repository = repository,
                side = DiffEntry.Side.NEW,
                entry = diffEntry,
                oldTreeIterator = null,
                newTreeIterator = null
            )

            if (entryContent !is EntryContent.Text)
                return@withContext

            val textLines = getLinesFromRawTextUseCase(entryContent.rawText).toMutableList()

            val hunkLines = hunk.lines.filter { it.lineType != LineType.CONTEXT }

            val addedLines = hunkLines
                .filter { it.lineType == LineType.ADDED }
                .sortedBy { it.newLineNumber }
            val removedLines = hunkLines
                .filter { it.lineType == LineType.REMOVED }
                .sortedBy { it.newLineNumber }

            var linesRemoved = 0

            // Start by removing the added lines to the index
            for (line in addedLines) {
                textLines.removeAt(line.newLineNumber + linesRemoved)
                linesRemoved--
            }

            var linesAdded = 0

            // Restore previously removed lines to the index
            for (line in removedLines) {
                // Check how many lines before this one have been deleted
                val previouslyRemovedLines = addedLines.count { it.newLineNumber < line.newLineNumber }
                textLines.add(line.newLineNumber + linesAdded - previouslyRemovedLines, line.text)
                linesAdded++
            }

            val stagedFileText = textLines.joinToString("")
            dirCacheEditor.add(HunkEdit(diffEntry.newPath, repository, ByteBuffer.wrap(stagedFileText.toByteArray())))
            dirCacheEditor.commit()

            completedWithErrors = false
        } finally {
            if (completedWithErrors)
                dirCache.unlock()
        }
    }
}