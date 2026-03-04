package com.jetpackduba.gitnuro.domain.git.workspace

import com.jetpackduba.gitnuro.domain.git.EntryContent
import com.jetpackduba.gitnuro.domain.git.RawFileManager
import com.jetpackduba.gitnuro.domain.git.diff.Hunk
import com.jetpackduba.gitnuro.domain.git.diff.Line
import com.jetpackduba.gitnuro.domain.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.nio.ByteBuffer
import javax.inject.Inject

class StageHunkLineGitAction @Inject constructor(
    private val rawFileManager: RawFileManager,
    private val getLinesFromRawTextGitAction: GetLinesFromRawTextGitAction,
) {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk, line: Line) =
        withContext(Dispatchers.IO) {
            val repository = git.repository
            val dirCache = repository.lockDirCache()
            val dirCacheEditor = dirCache.editor()
            var completedWithErrors = true

            try {
                val entryContent = rawFileManager.getRawContent(
                    repository = git.repository,
                    side = DiffEntry.Side.OLD,
                    entry = diffEntry,
                    oldTreeIterator = null,
                    newTreeIterator = null
                )

                if (entryContent !is EntryContent.Text)
                    return@withContext

                val textLines = getLinesFromRawTextGitAction(entryContent.rawText).toMutableList()

                when (line.lineType) {
                    LineType.ADDED -> {
                        val previousContextLine = hunk.lines
                            .takeWhile { it != line }
                            .lastOrNull { it.lineType == LineType.CONTEXT }

                        val startingIndex = previousContextLine?.oldLineNumber ?: -1

                        textLines.add(startingIndex + 1, line.text)
                    }

                    LineType.REMOVED -> {
                        textLines.removeAt(line.oldLineNumber)
                    }

                    else -> {}
                }

                val stagedFileText = textLines.joinToString("")
                dirCacheEditor.add(
                    HunkEdit(
                        diffEntry.newPath,
                        repository,
                        ByteBuffer.wrap(stagedFileText.toByteArray())
                    )
                )
                dirCacheEditor.commit()

                completedWithErrors = false
            } finally {
                if (completedWithErrors)
                    dirCache.unlock()
            }
        }
}

