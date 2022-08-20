package app.usecase

import app.git.diff.DiffResult
import app.git.diff.Line
import app.git.diff.LineType
import app.git.diff.SplitHunk
import javax.inject.Inject
import kotlin.math.abs

class GenerateSplitHunkFromDiffResultUseCase @Inject constructor() {
    operator fun invoke(diffFormat: DiffResult.Text): List<SplitHunk> {
        val unifiedHunksList = diffFormat.hunks
        val hunksList = mutableListOf<SplitHunk>()

        for (hunk in unifiedHunksList) {
            val lines = hunk.lines
            val newSideLines = mutableListOf<Line?>()
            val oldSideLines = mutableListOf<Line?>()

            var consecutiveChangedLines = 0

            for (line in lines) {
                when (line.lineType) {
                    LineType.CONTEXT -> {
                        if (consecutiveChangedLines != 0) {
                            fillWithNulls(oldSideLines, newSideLines, consecutiveChangedLines)
                            consecutiveChangedLines = 0
                        }

                        oldSideLines.add(line)
                        newSideLines.add(line)
                    }

                    LineType.ADDED -> {
                        consecutiveChangedLines++
                        newSideLines.add(line)
                    }

                    LineType.REMOVED -> {
                        consecutiveChangedLines--
                        oldSideLines.add(line)
                    }
                }
            }

            if (consecutiveChangedLines != 0) {
                fillWithNulls(oldSideLines, newSideLines, consecutiveChangedLines)
            }

            val newHunkLines = mutableListOf<Pair<Line?, Line?>>()

            for (i in 0 until newSideLines.count()) {
                val old = oldSideLines[i]
                val new = newSideLines[i]

                newHunkLines.add(old to new)
            }

            hunksList.add(SplitHunk(hunk, newHunkLines))
        }

        return hunksList
    }

    private fun fillWithNulls(
        oldSideLines: MutableList<Line?>,
        newSideLines: MutableList<Line?>,
        consecutiveChangedLines: Int,
    ) {
        check(consecutiveChangedLines != 0)

        val listToUpdate = if (consecutiveChangedLines > 0) {
            oldSideLines
        } else if (consecutiveChangedLines < 0) {
            newSideLines
        } else {
            null
        }

        repeat(abs(consecutiveChangedLines)) {
            listToUpdate?.add(null)
        }
    }
}