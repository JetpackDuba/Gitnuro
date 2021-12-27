package app.git.diff

import app.extensions.lineAt
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.patch.FileHeader.PatchType
import org.eclipse.jgit.storage.pack.PackConfig
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.WorkingTreeIterator
import org.eclipse.jgit.util.LfsFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_BINARY_FILE_THRESHOLD = PackConfig.DEFAULT_BIG_FILE_THRESHOLD

/**
 * Generator of [Hunk] lists from [DiffEntry]
 */
class HunkDiffGenerator(
    private val repository: Repository,
) : AutoCloseable {
    private var reader: ObjectReader? = null
    private lateinit var diffCfg: DiffConfig
    private lateinit var diffAlgorithm: DiffAlgorithm
    private var context = 3
    private var binaryFileThreshold = DEFAULT_BINARY_FILE_THRESHOLD
    private val outputStream = ByteArrayOutputStream() // Dummy output stream used for the diff formatter
    private val diffFormatter = DiffFormatter(outputStream).apply {
        setRepository(repository)
    }

    init {
        setReader(repository.newObjectReader(), repository.config)
    }

    private var source: ContentSource.Pair? = null
    private var quotePaths: Boolean? = null

    private fun setReader(reader: ObjectReader, cfg: Config) {
        close()
        this.reader = reader
        diffCfg = cfg.get(DiffConfig.KEY)
        if (quotePaths == null) {
            quotePaths = java.lang.Boolean
                .valueOf(
                    cfg.getBoolean(
                        ConfigConstants.CONFIG_CORE_SECTION,
                        ConfigConstants.CONFIG_KEY_QUOTE_PATH, true
                    )
                )
        }
        val cs = ContentSource.create(reader)
        source = ContentSource.Pair(cs, cs)
        diffAlgorithm = DiffAlgorithm.getAlgorithm(
            cfg.getEnum(
                ConfigConstants.CONFIG_DIFF_SECTION, null,
                ConfigConstants.CONFIG_KEY_ALGORITHM,
                SupportedAlgorithm.HISTOGRAM
            )
        )
    }

    override fun close() {
        reader?.close()
        outputStream.close()
    }

    fun scan(oldTreeIterator: AbstractTreeIterator, newTreeIterator: AbstractTreeIterator) {
        source = ContentSource.Pair(source(oldTreeIterator), source(newTreeIterator))
        diffFormatter.scan(oldTreeIterator, newTreeIterator)
    }

    private fun source(iterator: AbstractTreeIterator): ContentSource {
        return if (iterator is WorkingTreeIterator)
            ContentSource.create(iterator)
        else
                ContentSource.create(reader)
    }

    fun format(ent: DiffEntry): List<Hunk>  {
        val fileHeader = diffFormatter.toFileHeader(ent)
        val rawOld = getRawContent(DiffEntry.Side.OLD, ent)
        val rawNew = getRawContent(DiffEntry.Side.NEW, ent)
        return format(fileHeader, rawOld, rawNew)
    }

    private fun getRawContent(side: DiffEntry.Side, entry: DiffEntry): RawText {
        if (entry.getMode(side) === FileMode.MISSING) return RawText.EMPTY_TEXT
        if (entry.getMode(side).objectType != Constants.OBJ_BLOB) return RawText.EMPTY_TEXT

        val ldr = LfsFactory.getInstance().applySmudgeFilter(
            repository,
            source!!.open(side, entry), entry.diffAttribute
        )
        return RawText.load(ldr, binaryFileThreshold)
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
            var oldCurrentLine = max(0, curEdit.beginA - context)
            var newCurrentLine = max(0, curEdit.beginB - context)
            val oldEndLine = min(oldRawText.size(), endEdit.endA + context)
            val newEndLine = min(newRawText.size(), endEdit.endB + context)

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
                    lines.add(Line.AddedLine(lineText, newCurrentLine))

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
        return e[i].beginA - e[i - 1].endA <= 2 * context
    }

    private fun combineB(e: List<Edit>, i: Int): Boolean {
        return e[i].beginB - e[i - 1].endB <= 2 * context
    }

    private fun end(edit: Edit, a: Int, b: Int): Boolean {
        return edit.endA <= a && edit.endB <= b
    }
}