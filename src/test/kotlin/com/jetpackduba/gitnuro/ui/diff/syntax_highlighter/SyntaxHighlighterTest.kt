package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class SyntaxHighlighterTest {

    @Test
    fun syntaxHighlight() {
        val annotatedString = AnnotatedString.Builder()

        annotatedString.append("val variable = \"Hello ")
        annotatedString.append(AnnotatedString("World", SpanStyle(color = Color.Blue)))
        annotatedString.append("\"")

        val syntaxHighlighter = getSyntaxHighlighterFromExtension("kt")
        val result = syntaxHighlighter.syntaxHighlight(
            annotatedString.toAnnotatedString(),
            commentColor = Color.Green,
            keywordColor = Color.Cyan,
            annotationColor = Color.Yellow,
        )

        assertTrue(
            result.spanStyles.contains(
                AnnotatedString.Range(
                    SpanStyle(Color.Cyan),
                    start = 0,
                    end = 3,
                )
            )
        )
    }
}