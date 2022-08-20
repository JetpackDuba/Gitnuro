package app.usecase

import app.git.diff.DiffResult
import app.git.diff.Hunk
import app.git.diff.Line
import app.git.diff.LineType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerateSplitHunkFromDiffResultUseCaseTest {
    private val generateSplitHunkFromDiffResultUseCase = GenerateSplitHunkFromDiffResultUseCase()

    @Test
    fun invoke_empty_hunks_list() {
        val hunksList = mutableListOf<Hunk>()

        val diffResultText = DiffResult.Text(mockk(), hunksList)
        val splitHunkList = generateSplitHunkFromDiffResultUseCase(diffResultText)

        assertEquals(splitHunkList.count(), 0)
    }

    @Test
    fun invoke_hunk_with_only_deleted_lines() {
        val hunksList = mutableListOf<Hunk>()
        val lines = listOf(
            Line("", 0, 0, LineType.CONTEXT),
            Line("", 1, 1, LineType.CONTEXT),
            Line("", 2, 2, LineType.REMOVED),
            Line("", 3, 3, LineType.REMOVED),
            Line("", 4, 2, LineType.CONTEXT),
            Line("", 5, 3, LineType.CONTEXT),
        )
        val hunk = Hunk(
            header = "",
            lines = lines,
        )

        hunksList.add(hunk)

        val diffResultText = DiffResult.Text(mockk(), hunksList)
        val splitHunkList = generateSplitHunkFromDiffResultUseCase(diffResultText)

        assertEquals(splitHunkList.count(), 1)

        val firstHunk = splitHunkList.first()
        assertEquals(firstHunk.lines.count(), 6)
        verifyContextLines(lines, firstHunk.lines)
        verifyContainsRemoved(listOf(2, 3), firstHunk.lines)
        verifyContainsNullNewSide(listOf(2, 3), firstHunk.lines)
    }

    @Test
    fun invoke_hunk_with_mix_lines_and_new_side_with_nulls() {
        val hunksList = mutableListOf<Hunk>()
        val lines = listOf(
            Line("", 0, 0, LineType.CONTEXT),
            Line("", 1, 1, LineType.CONTEXT),
            Line("", 2, 2, LineType.REMOVED),
            Line("", 3, 3, LineType.REMOVED),
            Line("", 2, 2, LineType.ADDED),
            Line("", 4, 3, LineType.CONTEXT),
            Line("", 5, 4, LineType.CONTEXT),
        )
        val hunk = Hunk(
            header = "",
            lines = lines,
        )

        hunksList.add(hunk)

        val diffResultText = DiffResult.Text(mockk(), hunksList)
        val splitHunkList = generateSplitHunkFromDiffResultUseCase(diffResultText)

        assertEquals(splitHunkList.count(), 1)

        val firstHunk = splitHunkList.first()
        assertEquals(firstHunk.lines.count(), 6)
        verifyContextLines(lines, firstHunk.lines)
        verifyContainsRemoved(listOf(2, 3), firstHunk.lines)
        verifyContainsAdded(listOf(2), firstHunk.lines)
        verifyContainsNullNewSide(listOf(3), firstHunk.lines)
    }

    @Test
    fun invoke_hunk_with_mix_lines_and_old_side_with_nulls() {
        val hunksList = mutableListOf<Hunk>()
        val lines = listOf(
            Line("", 0, 0, LineType.CONTEXT),
            Line("", 1, 1, LineType.CONTEXT),
            Line("", 2, 2, LineType.CONTEXT),
            Line("", 3, 3, LineType.REMOVED),
            Line("", 2, 3, LineType.ADDED),
            Line("", 3, 4, LineType.ADDED),
            Line("", 3, 5, LineType.ADDED),
            Line("", 4, 6, LineType.CONTEXT),
            Line("", 5, 7, LineType.CONTEXT),
        )
        val hunk = Hunk(
            header = "",
            lines = lines,
        )

        hunksList.add(hunk)

        val diffResultText = DiffResult.Text(mockk(), hunksList)
        val splitHunkList = generateSplitHunkFromDiffResultUseCase(diffResultText)

        assertEquals(splitHunkList.count(), 1)

        val firstHunk = splitHunkList.first()
        assertEquals(firstHunk.lines.count(), 8)
        verifyContextLines(lines, firstHunk.lines)
        verifyContainsRemoved(listOf(3), firstHunk.lines)
        verifyContainsAdded(listOf(3, 4), firstHunk.lines)
        verifyContainsNullOldSide(listOf(4, 5), firstHunk.lines)
    }

    @Test
    fun invoke_hunk_with_mix_lines_and_old_side_with_context_in_between() {
        val hunksList = mutableListOf<Hunk>()
        val lines = listOf(
            Line("", 0, 0, LineType.CONTEXT),
            Line("", 1, 1, LineType.CONTEXT),
            Line("", 2, 2, LineType.REMOVED),
            Line("", 3, 2, LineType.CONTEXT),
            Line("", 3, 3, LineType.ADDED),
            Line("", 4, 4, LineType.CONTEXT),
            Line("", 5, 5, LineType.CONTEXT),
        )
        val hunk = Hunk(
            header = "",
            lines = lines,
        )

        hunksList.add(hunk)

        val diffResultText = DiffResult.Text(mockk(), hunksList)
        val splitHunkList = generateSplitHunkFromDiffResultUseCase(diffResultText)

        assertEquals(splitHunkList.count(), 1)

        val firstHunk = splitHunkList.first()
        assertEquals(firstHunk.lines.count(), 7)
        verifyContextLines(lines, firstHunk.lines)
        verifyContainsRemoved(listOf(2), firstHunk.lines)
        verifyContainsAdded(listOf(4), firstHunk.lines)
        verifyContainsNullOldSide(listOf(4), firstHunk.lines)
        verifyContainsNullNewSide(listOf(2), firstHunk.lines)
    }

    private fun verifyContextLines(sourceLines: List<Line>, linesToTest: List<Pair<Line?, Line?>>) {
        val contextSourceLines = sourceLines.filter { it.lineType == LineType.CONTEXT }
        val contextLinesToTest = linesToTest.filter { it.first?.lineType == LineType.CONTEXT }

        contextSourceLines.forEachIndexed { index, line ->
            val linePair = contextLinesToTest[index]
            assertEquals(linePair.first, line)
            assertEquals(linePair.second, line)

        }
    }

    private fun verifyContainsRemoved(removedLinesIndexes: List<Int>, linesToTest: List<Pair<Line?, Line?>>) {
        removedLinesIndexes.forEach { lineNumber ->
            assertEquals(linesToTest[lineNumber].first?.lineType, LineType.REMOVED)
        }
    }

    private fun verifyContainsAdded(removedLinesIndexes: List<Int>, linesToTest: List<Pair<Line?, Line?>>) {
        removedLinesIndexes.forEach { lineNumber ->
            assertEquals(linesToTest[lineNumber].second?.lineType, LineType.ADDED)
        }
    }

    private fun verifyContainsNullOldSide(removedLinesIndexes: List<Int>, linesToTest: List<Pair<Line?, Line?>>) {
        removedLinesIndexes.forEach { lineNumber ->
            assertEquals(linesToTest[lineNumber].first, null)
        }
    }

    private fun verifyContainsNullNewSide(removedLinesIndexes: List<Int>, linesToTest: List<Pair<Line?, Line?>>) {
        removedLinesIndexes.forEach { lineNumber ->
            assertEquals(linesToTest[lineNumber].second, null)
        }
    }
}