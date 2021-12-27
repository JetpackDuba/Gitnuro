package app.git.diff

import app.extensions.lineAt
import app.git.RawFileManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.patch.FileHeader.PatchType
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

private const val CONTEXT_LINES = 3
/**
 * Generator of [Hunk] lists from [DiffEntry]
 */
class HunkDiffGenerator @AssistedInject constructor(
    @Assisted private val repository: Repository,
    @Assisted private val rawFileManager: RawFileManager,
) : AutoCloseable {

    private val outputStream = ByteArrayOutputStream() // Dummy output stream used for the diff formatter
    private val diffFormatter = DiffFormatter(outputStream).apply {
        setRepository(repository)
    }

    override fun close() {
        outputStream.close()
    }

    fun scan(oldTreeIterator: AbstractTreeIterator, newTreeIterator: AbstractTreeIterator) {
        rawFileManager.scan(oldTreeIterator, newTreeIterator)
        diffFormatter.scan(oldTreeIterator, newTreeIterator)
    }

    fun format(ent: DiffEntry): List<Hunk>  {
        val fileHeader = diffFormatter.toFileHeader(ent)
        val rawOld = rawFileManager.getRawContent(DiffEntry.Side.OLD, ent)
        val rawNew = rawFileManager.getRawContent(DiffEntry.Side.NEW, ent)
        return format(fileHeader, rawOld, rawNew)
    }

    /**
     * Given a [FileHeader] and the both [RawText], generate a [List] of [Hunk]
     */
    private fun format(head: FileHeader, oldRawText: RawText, newRawText: RawText): List<Hunk> {
        return if (head.patchType == PatchType.UNIFIED)
            format(head.toEditList(), oldRawText, newRawText)
        else
            emptyList()
    }

    private fun format(edits: EditList, oldRawText: RawText, newRawText: RawText): List<Hunk> {
        var curIdx = 0
        val hunksList = mutableListOf<Hunk>()
        while (curIdx < edits.size) {
            var curEdit = edits[curIdx]
            val endIdx = findCombinedEnd(edits, curIdx)
            val endEdit = edits[endIdx]
            var oldCurrentLine = max(0, curEdit.beginA - CONTEXT_LINES)
            var newCurrentLine = max(0, curEdit.beginB - CONTEXT_LINES)
            val oldEndLine = min(oldRawText.size(), endEdit.endA + CONTEXT_LINES)
            val newEndLine = min(newRawText.size(), endEdit.endB + CONTEXT_LINES)

            val headerText = createHunkHeader(oldCurrentLine, oldEndLine, newCurrentLine, newEndLine)
            val lines = mutableListOf<Line>()

            while (oldCurrentLine < oldEndLine || newCurrentLine < newEndLine) {
                if (oldCurrentLine < curEdit.beginA || endIdx + 1 < curIdx) {
                    val lineText = oldRawText.lineAt(oldCurrentLine)
                    lines.add(Line.ContextLine(lineText, oldCurrentLine, newCurrentLine))

                    oldCurrentLine++
                    newCurrentLine++
                } else if (oldCurrentLine < curEdit.endA) {
                    val lineText = oldRawText.lineAt(oldCurrentLine)
                    lines.add(Line.RemovedLine(lineText, oldCurrentLine))

                    oldCurrentLine++
                } else if (newCurrentLine < curEdit.endB) {
                    val lineText = newRawText.lineAt(newCurrentLine)
                    lines.add(Line.AddedLine(lineText, oldCurrentLine, newCurrentLine))

                    newCurrentLine++
                }

                if (end(curEdit, oldCurrentLine, newCurrentLine) && ++curIdx < edits.size) curEdit = edits[curIdx]
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
        newEndLine: Int
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
        while (end < edits.size
            && (combineA(edits, end) || combineB(edits, end))
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