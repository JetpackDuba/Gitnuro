package app.usecase

import app.extensions.matchingIndexes
import app.git.diff.DiffResult
import app.git.diff.Line
import app.git.diff.LineType
import app.git.diff.SplitHunk
import javax.inject.Inject

class GenerateSplitHunkFromDiffResultUseCase @Inject constructor() {
    operator fun invoke(diffFormat: DiffResult.Text): List<SplitHunk> {
        val unifiedHunksList = diffFormat.hunks
        val hunksList = mutableListOf<SplitHunk>()

        for (hunk in unifiedHunksList) {
            val lines = hunk.lines

            val linesNewSideCount =
                lines.count { it.lineType == LineType.ADDED || it.lineType == LineType.CONTEXT }
            val linesOldSideCount =
                lines.count { it.lineType == LineType.REMOVED || it.lineType == LineType.CONTEXT }

            val addedLines = lines.filter { it.lineType == LineType.ADDED }
            val removedLines = lines.filter { it.lineType == LineType.REMOVED }

            val oldLinesArray: Array<Line?> = if (linesNewSideCount > linesOldSideCount)
                generateArrayWithContextLines(
                    hunkLines = lines,
                    linesCount = linesNewSideCount,
                    lineNumberCallback = { it.newLineNumber },
                )
            else
                generateArrayWithContextLines(
                    hunkLines = lines,
                    linesCount = linesOldSideCount,
                    lineNumberCallback = { it.oldLineNumber },
                )

            // Old lines array only contains context lines for now, so copy it to new lines array
            val newLinesArray = oldLinesArray.copyOf()

            val arraysSize = newLinesArray.count()

            for (removedLine in removedLines) {
                placeLine(oldLinesArray, lines, removedLine)
            }

            for (addedLine in addedLines) {
                placeLine(newLinesArray, lines, addedLine)
            }

            val newHunkLines = mutableListOf<Pair<Line?, Line?>>()

            for (i in 0 until arraysSize) {
                val old = oldLinesArray[i]
                val new = newLinesArray[i]

                newHunkLines.add(old to new)
            }

            hunksList.add(SplitHunk(hunk, newHunkLines))
        }

        return hunksList
    }

    private inline fun generateArrayWithContextLines(
        hunkLines: List<Line>,
        lineNumberCallback: (Line) -> Int,
        linesCount: Int
    ): Array<Line?> {
        val linesArray = arrayOfNulls<Line?>(linesCount)

        val contextLines = hunkLines.filter { it.lineType == LineType.CONTEXT }

        val firstLine = hunkLines.firstOrNull()
        val firstLineNumber = if (firstLine == null) {
            0
        } else
            lineNumberCallback(firstLine)

        for (contextLine in contextLines) {
            val lineNumber = lineNumberCallback(contextLine)

            linesArray[lineNumber - firstLineNumber] = contextLine
        }

        return linesArray
    }

    private fun placeLine(linesArray: Array<Line?>, hunkLines: List<Line>, lineToPlace: Line) {
        val previousLinesToCurrent = hunkLines.takeWhile { it != lineToPlace }
        val previousContextLine = previousLinesToCurrent.lastOrNull { it.lineType == LineType.CONTEXT }

        val contextArrayPosition = if (previousContextLine != null)
            linesArray.indexOf(previousContextLine)
        else
            -1

        val availableIndexes = linesArray.matchingIndexes { it == null }

        // Get the position of the next available line after the previous context line
        val nextAvailableLinePosition = availableIndexes.first { index -> index > contextArrayPosition }

        linesArray[nextAvailableLinePosition] = lineToPlace
    }
}