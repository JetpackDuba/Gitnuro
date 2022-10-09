package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.RawFileManager
import com.jetpackduba.gitnuro.git.diff.Hunk
import com.jetpackduba.gitnuro.git.diff.Line
import com.jetpackduba.gitnuro.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.nio.ByteBuffer
import javax.inject.Inject

class StageHunkLineUseCase @Inject constructor(
    private val rawFileManager: RawFileManager,
    private val getLinesFromRawTextUseCase: GetLinesFromRawTextUseCase,
) {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk, lineToStage: Line) =
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

                val textLines = getLinesFromRawTextUseCase(entryContent.rawText).toMutableList()


                when (lineToStage.lineType) {
                    LineType.ADDED -> {
                        val previousContextLine = hunk.lines
                            .takeWhile { it != lineToStage }
                            .lastOrNull { it.lineType == LineType.CONTEXT }

                        val startingIndex = previousContextLine?.oldLineNumber ?: -1

                        textLines.add(startingIndex + 1, lineToStage.text)
                    }

                    LineType.REMOVED -> {
                        textLines.removeAt(lineToStage.oldLineNumber)
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

