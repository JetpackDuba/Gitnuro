package app.git.workspace

import app.git.EntryContent
import app.git.RawFileManager
import app.git.diff.Hunk
import app.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.nio.ByteBuffer
import javax.inject.Inject

class UnstageHunkUseCase @Inject constructor(
    private val rawFileManager: RawFileManager,
    private val getLinesFromRawTextUseCase: GetLinesFromRawTextUseCase
) {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
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

            val hunkLines = hunk.lines.filter { it.lineType != LineType.CONTEXT }

            var linesAdded = 0
            for (line in hunkLines) {
                when (line.lineType) {
                    LineType.ADDED -> {
                        textLines.add(line.oldLineNumber + linesAdded, line.text)
                        linesAdded++
                    }

                    LineType.REMOVED -> {
                        textLines.removeAt(line.oldLineNumber + linesAdded)
                        linesAdded--
                    }

                    else -> throw NotImplementedError("Line type not implemented for stage hunk")
                }
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