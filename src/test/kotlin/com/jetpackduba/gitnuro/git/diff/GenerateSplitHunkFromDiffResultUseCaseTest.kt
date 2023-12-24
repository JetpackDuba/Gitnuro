package com.jetpackduba.gitnuro.git.diff

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GenerateSplitHunkFromDiffResultUseCaseTest {

    private val generateSplitHunkFromDiffResultUseCase = GenerateSplitHunkFromDiffResultUseCase()

    @Test
    fun `generate splitHunk from hunk`() {
        val lines = listOf(
            Line(
                text = "    operator fun invoke() {",
                oldLineNumber = 1,
                newLineNumber = 1,
                LineType.CONTEXT,
            ),
            Line(
                text = "    operator fun invoke() {",
                oldLineNumber = 2,
                newLineNumber = 2,
                LineType.REMOVED,
            ),
            Line(
                text = "    operator fun invoke() {",
                oldLineNumber = 2,
                newLineNumber = 2,
                LineType.ADDED,
            ),
            Line(
                text = "    operator fun invoke() {",
                oldLineNumber = 2,
                newLineNumber = 3,
                LineType.ADDED,
            ),
            Line(
                text = "    operator fun invoke() {",
                oldLineNumber = 3,
                newLineNumber = 4,
                LineType.CONTEXT,
            )
        )
        val hunks = listOf(
            Hunk(
                header = "",
                lines = lines
            )
        )

        val diffResultText = DiffResult.Text(mockk(), hunks)
        val splitDiff = generateSplitHunkFromDiffResultUseCase(diffResultText)
        val splitHunk = splitDiff.first()
        val splitHunkLines = splitHunk.lines

        splitHunkLines[0].apply {
            assertEquals(first, lines[0])
            assertEquals(second, lines[0])
        }

        splitHunkLines[1].apply {
            assertEquals(first, lines[1])
            assertEquals(second, lines[2])
        }

        splitHunkLines[2].apply {
            assertEquals(first, null)
            assertEquals(second, lines[3])
        }

        splitHunkLines[3].apply {
            assertEquals(first, lines[4])
            assertEquals(second, lines[4])
        }


    }
}