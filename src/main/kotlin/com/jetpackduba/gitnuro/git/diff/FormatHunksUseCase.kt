package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.extensions.lineAt
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.patch.FileHeader.PatchType
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private const val CONTEXT_LINES = 3

/**
 * Generator of [Hunk] lists from [DiffEntry]
 */
class FormatHunksUseCase @Inject constructor() {
    operator fun invoke(
        fileHeader: FileHeader,
        rawOld: RawText,
        rawNew: RawText,
        isDisplayFullFile: Boolean,
    ): List<Hunk> {
        return if (fileHeader.patchType == PatchType.UNIFIED)
            format(fileHeader.toEditList(), rawOld, rawNew, isDisplayFullFile)
        else
            emptyList()
    }

    private fun format(
        edits: EditList,
        oldRawText: RawText,
        newRawText: RawText,
        isDisplayFullFile: Boolean,
    ): List<Hunk> {
        var curIdx = 0
        val hunksList = mutableListOf<Hunk>()
        while (curIdx < edits.count()) {
            var curEdit = edits[curIdx]

            val endIdx: Int = if (isDisplayFullFile)
                edits.lastIndex
            else
                findCombinedEnd(edits, curIdx)

            val endEdit = edits[endIdx]

            var oldCurrentLine: Int
            var newCurrentLine: Int
            val oldEndLine: Int
            val newEndLine: Int

            if (isDisplayFullFile) {
                oldCurrentLine = 0
                newCurrentLine = 0
                oldEndLine = oldRawText.size()
                newEndLine = newRawText.size()
            } else {
                oldCurrentLine = max(0, curEdit.beginA - CONTEXT_LINES)
                newCurrentLine = max(0, curEdit.beginB - CONTEXT_LINES)
                oldEndLine = min(oldRawText.size(), endEdit.endA + CONTEXT_LINES)
                newEndLine = min(newRawText.size(), endEdit.endB + CONTEXT_LINES)
            }

            val headerText = createHunkHeader(oldCurrentLine, oldEndLine, newCurrentLine, newEndLine)
            val lines = mutableListOf<Line>()

            while (oldCurrentLine < oldEndLine || newCurrentLine < newEndLine) {
                if (oldCurrentLine < curEdit.beginA || endIdx + 1 < curIdx) {
                    val lineText = oldRawText.lineAt(oldCurrentLine)

                    lines.add(Line(lineText, oldCurrentLine, newCurrentLine, LineType.CONTEXT))

                    oldCurrentLine++
                    newCurrentLine++
                } else if (oldCurrentLine < curEdit.endA) {
                    val lineText = oldRawText.lineAt(oldCurrentLine)

                    lines.add(Line(lineText, oldCurrentLine, newCurrentLine, LineType.REMOVED))

                    oldCurrentLine++
                } else if (newCurrentLine < curEdit.endB) {
                    val lineText = newRawText.lineAt(newCurrentLine)

                    lines.add(Line(lineText, oldCurrentLine, newCurrentLine, LineType.ADDED))

                    newCurrentLine++
                }

                if (end(curEdit, oldCurrentLine, newCurrentLine) && ++curIdx < edits.size) {
                    curEdit = edits[curIdx]
                }
            }

            hunksList.add(Hunk(headerText, lines))
        }

        return hunksList
    }

    /**
     * Generates the hunk's header string like in git diff
     */
    @Throws(IOException::class)
    private fun createHunkHeader(
        oldStartLine: Int,
        oldEndLine: Int,
        newStartLine: Int,
        newEndLine: Int,
    ): String {
        val prefix = "@@"
        val contentRemoved = createRange('-', oldStartLine + 1, oldEndLine - oldStartLine)
        val contentAdded = createRange('+', newStartLine + 1, newEndLine - newStartLine)
        val suffix = " @@"

        return prefix + contentRemoved + contentAdded + suffix
    }

    private fun createRange(symbol: Char, begin: Int, linesCount: Int): String {
        return when (linesCount) {
            0 -> " $symbol${begin - 1},0"
            1 -> " $symbol$begin" // If the range is exactly one line, produce only the number.
            else -> " $symbol$begin,$linesCount"
        }
    }

    private fun findCombinedEnd(edits: List<Edit>, i: Int): Int {
        var end = i + 1

        while (
            end < edits.size &&
            (combineA(edits, end) || combineB(edits, end))
        ) end++
        return end - 1
    }

    private fun combineA(e: List<Edit>, i: Int): Boolean {
        return e[i].beginA - e[i - 1].endA <= 2 * CONTEXT_LINES
    }

    private fun combineB(e: List<Edit>, i: Int): Boolean {
        return e[i].beginB - e[i - 1].endB <= 2 * CONTEXT_LINES
    }

    private fun end(edit: Edit, a: Int, b: Int): Boolean {
        return edit.endA <= a && edit.endB <= b
    }
}

