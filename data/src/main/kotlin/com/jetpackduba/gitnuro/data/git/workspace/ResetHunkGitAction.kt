package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IResetHunkGitAction
import com.jetpackduba.gitnuro.domain.models.Hunk
import com.jetpackduba.gitnuro.domain.models.LineType
import org.eclipse.jgit.diff.DiffEntry
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class ResetHunkGitAction @Inject constructor(
    private val jgit: JGit,
    private val getLinesFromTextGitAction: GetLinesFromTextGitAction,
) : IResetHunkGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        diffEntry: DiffEntry,
        hunk: Hunk
    ): Either<Unit, GitError> =
        jgit.provide(repositoryPath) { git ->
            val repository = git.repository

            val file = File(repository.workTree, diffEntry.oldPath)

            val content = file.readText()
            val textLines = getLinesFromTextGitAction(content).toMutableList()
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
        }
}