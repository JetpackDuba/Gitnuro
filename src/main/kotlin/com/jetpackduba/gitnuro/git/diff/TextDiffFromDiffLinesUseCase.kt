package com.jetpackduba.gitnuro.git.diff

import javax.inject.Inject
import kotlin.math.min

class TextDiffFromDiffLinesUseCase @Inject constructor() {
    operator fun invoke(lines: List<Line>): List<Line> {
        val linesToDiff = mutableListOf<Line>()

        var contextCount = 0

        val lastNonContextIndex = lines.indexOfLast { it.lineType != LineType.CONTEXT }

        val newLines = mutableListOf<Line>()

        lines.forEachIndexed { index, line ->
            if (line.lineType == LineType.CONTEXT) {
                contextCount++
            } else {
                contextCount = 0
            }

            if (!(linesToDiff.isEmpty() && line.lineType == LineType.CONTEXT)) {
                linesToDiff.add(line)
            } else {
                newLines.add(line)
            }

            if (
                (contextCount >= 1 && linesToDiff.count() > 1) ||
                (index == lines.lastIndex || index == lastNonContextIndex)
            ) {
                if (linesToDiff.isNotEmpty()) {
                    val ne = diffHunkLines(linesToDiff.trimContextLines())
                    newLines.addAll(ne)

                    if (line.lineType == LineType.CONTEXT) {
                        newLines.add(line)
                    }

                    linesToDiff.clear()
                }

                contextCount = 0
            }
        }

        return newLines
    }

    private fun List<Line>.trimContextLines(): List<Line> {
        val firsNonContextIndex = indexOfFirst { it.lineType != LineType.CONTEXT }
        val lastNonContextIndex = indexOfLast { it.lineType != LineType.CONTEXT }

        return subList(firsNonContextIndex, min(count(), lastNonContextIndex + 1))
    }

    private fun diffHunkLines(lines: List<Line>): List<Line> {

        val oldText = lines
            .filter { it.lineType == LineType.CONTEXT || it.lineType == LineType.REMOVED }
            .joinToString("") { it.text }

        val newText = lines
            .filter { it.lineType == LineType.CONTEXT || it.lineType == LineType.ADDED }
            .joinToString("") { it.text }

        var oldIndex = 0
        var newIndex = 0

        val dmp = DiffMatchPatch()

        val diffs = dmp.diffMain(oldText, newText, false)

        dmp.diffCleanupSemantic(diffs)

        val mapping = diffs
            .asSequence()
            .map { diff ->
                val endsInNewLine = diff.text.endsWith("\n")

                val parts = diff.text
                    .split("\n")
                    .run {
                        if (endsInNewLine && this.isNotEmpty()) {
                            this.subList(0, this.count() - 1)
                        } else {
                            this
                        }
                    }

                parts
                    .mapIndexed { index: Int, s: String ->
                        val part = if (index == parts.lastIndex && !endsInNewLine) {
                            DiffMatchPatch.Diff(diff.operation, s)
                        } else {
                            DiffMatchPatch.Diff(diff.operation, s + "\n")
                        }

                        part
                    }
            }
            .fold(DiffAcc()) { accumulator, newDiffs ->
                for (newDiff in newDiffs) {
                    addOldLine(accumulator, newDiff)
                    addNewLine(accumulator, newDiff)
                }

                accumulator
            }

        val oldMatchLines = mapping.oldMatchLineDiffs

        val newMatchLines = mapping.newLinesDiffs

        val newLines = lines.map { line ->
            when (line.lineType) {
                LineType.CONTEXT -> {
                    line
                }

                LineType.ADDED -> {
                    if (newMatchLines.isEmpty()) {
                        line
                    } else {
                        val newLine = line.copy(
                            textDiffed = newMatchLines[newIndex]
                        )

                        newIndex++

                        newLine
                    }
                }

                LineType.REMOVED -> {
                    if (oldMatchLines.isEmpty()) {
                        line
                    } else {
                        val oldLine = line.copy(
                            textDiffed = oldMatchLines[min(oldMatchLines.lastIndex, oldIndex)]
                        )

                        oldIndex++

                        oldLine
                    }
                }
            }
        }

        return newLines
    }
}


fun addOldLine(
    diffAcc: DiffAcc,
    newValue: DiffMatchPatch.Diff,
) {
    val oldLineDiffs = diffAcc.oldMatchLineDiffs
    val oldLastLine = oldLineDiffs.lastOrNull()

    if (
        oldLastLine != null &&
        oldLastLine.diffs.lastOrNull()?.text?.endsWith("\n") == false &&
        (newValue.operation == DiffMatchPatch.Operation.DELETE || newValue.operation == DiffMatchPatch.Operation.EQUAL)
    ) {
        oldLineDiffs[oldLineDiffs.lastIndex] = oldLastLine.copy(diffs = oldLastLine.diffs + newValue)
    } else if (
        (newValue.operation == DiffMatchPatch.Operation.DELETE || newValue.operation == DiffMatchPatch.Operation.EQUAL)
    ) {
        oldLineDiffs.add(MatchLine(listOf(newValue)))
    }
}

fun addNewLine(
    diffAcc: DiffAcc,
    newValue: DiffMatchPatch.Diff,
) {
    val newLineDiffs = diffAcc.newLinesDiffs
    val newLastLine = newLineDiffs.lastOrNull()

    if (
        newLastLine != null &&
        newLastLine.diffs.lastOrNull()?.text?.endsWith("\n") == false &&
        (newValue.operation == DiffMatchPatch.Operation.INSERT || newValue.operation == DiffMatchPatch.Operation.EQUAL)
    ) {
        newLineDiffs[newLineDiffs.lastIndex] = newLastLine.copy(newLastLine.diffs + newValue)
    } else if (
        (newValue.operation == DiffMatchPatch.Operation.INSERT || newValue.operation == DiffMatchPatch.Operation.EQUAL)
    ) {
        newLineDiffs.add(MatchLine(listOf(newValue)))
    }
}

class DiffAcc {
    val oldMatchLineDiffs = mutableListOf<MatchLine>()
    val newLinesDiffs = mutableListOf<MatchLine>()
}


data class MatchLine(val diffs: List<DiffMatchPatch.Diff>)