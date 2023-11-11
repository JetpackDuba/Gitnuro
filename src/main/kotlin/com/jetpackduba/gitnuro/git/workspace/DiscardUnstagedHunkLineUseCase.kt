package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.git.diff.Hunk
import com.jetpackduba.gitnuro.git.diff.Line
import com.jetpackduba.gitnuro.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.io.File
import javax.inject.Inject

class DiscardUnstagedHunkLineUseCase @Inject constructor(
    private val getLinesFromTextUseCase: GetLinesFromTextUseCase,
) {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk, line: Line) =
        withContext(Dispatchers.IO) {
            val repository = git.repository

            try {
                val file = File(repository.workTree, diffEntry.filePath)
                val content = file.readText(Charsets.UTF_8)
                val textLines = getLinesFromTextUseCase(content).toMutableList()

                if (line.lineType == LineType.ADDED) {
                    textLines.removeAt(line.newLineNumber)
                } else if (line.lineType == LineType.REMOVED) {
                    val previousContextLine = hunk.lines
                        .takeWhile { it != line }
                        .lastOrNull { it.lineType == LineType.CONTEXT }

                    if (previousContextLine != null) {
                        textLines.add(previousContextLine.newLineNumber + 1, line.text)
                    } else {
                        val previousAddedLine = hunk.lines
                            .takeWhile { it != line }
                            .lastOrNull { it.lineType == LineType.ADDED }

                        if (previousAddedLine != null) {
                            textLines.add(previousAddedLine.newLineNumber + 1, line.text)
                        } else {
                            textLines.add(0, line.text)
                        }
                    }
                }

                val resultText = textLines.joinToString("")
                file.writeText(resultText, Charsets.UTF_8)

            } catch (ex: Exception) {
                throw Exception(
                    "Discard hunk line failed. Check if the file still exists and has the write permissions set",
                    ex
                )
            }
        }
}

