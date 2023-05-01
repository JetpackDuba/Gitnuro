package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.lineDelimiter
import com.jetpackduba.gitnuro.git.diff.Hunk
import com.jetpackduba.gitnuro.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class ResetHunkUseCase @Inject constructor(
    private val getLinesFromTextUseCase: GetLinesFromTextUseCase,
) {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk) = withContext(Dispatchers.IO) {
        val repository = git.repository

        try {
            val file = File(repository.workTree, diffEntry.oldPath)

            val content = file.readText()
            val textLines = getLinesFromTextUseCase(content).toMutableList() // TODO Test this
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


            FileWriter(file).use { fw ->
                fw.write(stagedFileText)
            }
        } catch (ex: Exception) {
            throw Exception("Discard hunk failed. Check if the file still exists and has the write permissions set", ex)
        }
    }
}