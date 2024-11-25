package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.jetpackduba.gitnuro.extensions.removeLineDelimiters
import com.jetpackduba.gitnuro.extensions.replaceTabs
import com.jetpackduba.gitnuro.git.diff.DiffMatchPatch
import com.jetpackduba.gitnuro.git.diff.MatchLine
import com.jetpackduba.gitnuro.theme.diffAnnotation
import com.jetpackduba.gitnuro.theme.diffComment
import com.jetpackduba.gitnuro.theme.diffKeyword

abstract class SyntaxHighlighter {
    private val keywords: List<String> by lazy {
        loadKeywords()
    }

    fun syntaxHighlight(
        annotatedString: AnnotatedString,
        commentColor: Color,
        keywordColor: Color,
        annotationColor: Color,
    ): AnnotatedString {
        val cleanText = annotatedString.text

        var iteratedCharsCount = 0
        val builder = AnnotatedString.Builder()
        builder.append(cleanText)

        for (spanStyleRange in annotatedString.spanStyles) {
            builder.addStyle(spanStyleRange.item, spanStyleRange.start, spanStyleRange.end)
        }

        if (isComment(cleanText.trimStart())) {
            builder.addStyle(
                style = SpanStyle(color = commentColor),
                start = 0,
                end = cleanText.count(),
            )
        } else {
            val words = cleanText.split(" ")

            words.forEachIndexed { index, word ->
                val start = iteratedCharsCount
                val end = iteratedCharsCount + word.count()

                if (keywords.contains(word)) {
                    builder.addStyle(
                        style = SpanStyle(
                            color = keywordColor,
                        ),
                        start = start,
                        end = end,
                    )
                } else if (isAnnotation(word)) {
                    builder.addStyle(
                        style = SpanStyle(
                            color = annotationColor,
                        ),
                        start = start,
                        end = end,
                    )
                }

                iteratedCharsCount += word.count() + 1

                if (index == words.lastIndex) {
                    iteratedCharsCount--
                }
            }
        }

        return builder.toAnnotatedString()
    }

    @Composable
    fun syntaxHighlight(text: String): AnnotatedString {
        val cleanText = text.replace(
            "\t",
            "    "
        ).removeLineDelimiters()

        return if (isComment(cleanText.trimStart())) {
            AnnotatedString(cleanText, spanStyle = SpanStyle(color = MaterialTheme.colors.diffComment))
        } else {
            val words = cleanText.split(" ")

            val builder = AnnotatedString.Builder()

            words.forEachIndexed { index, word ->
                if (keywords.contains(word)) {
                    builder.append(
                        AnnotatedString(
                            word,
                            spanStyle = SpanStyle(
                                color = MaterialTheme.colors.diffKeyword
                            )
                        )
                    )
                } else if (isAnnotation(word)) {
                    builder.append(
                        AnnotatedString(
                            word,
                            spanStyle = SpanStyle(
                                color = MaterialTheme.colors.diffAnnotation
                            )
                        )
                    )
                } else {
                    builder.append(word)
                }

                if (index < words.lastIndex) {
                    builder.append(" ")
                }
            }

            builder.toAnnotatedString()
        }
    }

    abstract fun isAnnotation(word: String): Boolean
    abstract fun isComment(line: String): Boolean
    abstract fun loadKeywords(): List<String>
}

fun getSyntaxHighlighterFromExtension(extension: String?): SyntaxHighlighter {
    val matchingHighlightLanguage = HighlightLanguagesSupported.entries.firstOrNull { language ->
        language.extensions.contains(extension)
    }

    return matchingHighlightLanguage?.highlighter?.invoke() ?: DefaultSyntaxHighlighter()
}

private enum class HighlightLanguagesSupported(val extensions: List<String>, val highlighter: () -> SyntaxHighlighter) {
    Kotlin(listOf("kt", "kts"), { KotlinSyntaxHighlighter() }),
    Rust(listOf("rs"), { RustSyntaxHighlighter() }),
    TypeScript(listOf("js", "jsx", "ts", "tsx", "vue", "astro", "svelte"), { TypeScriptSyntaxHighlighter() }), // JS & various frameworks files also included
    Python(listOf("py"), { PythonSyntaxHighlighter() }),
    Java(listOf("java"), { JavaSyntaxHighlighter() }),
    CSharp(listOf("cs"), { CSharpSyntaxHighlighter() }),
    Cpp(listOf("c", "cpp", "c", "h", "hh", "hpp"), { CppSyntaxHighlighter() }), // C files also included
    PHP(listOf("php"), { PhpSyntaxHighlighter() }),
    SQL(listOf("sql"), { SQLSyntaxHighlighter() }),
    Ruby(listOf("rb"), { RubySyntaxHighlighter() }),
    Go(listOf("go"), { GoSyntaxHighlighter() }),
    Dart(listOf("dart"), { DartSyntaxHighlighter() }),
    Swift(listOf("swift"), { SwiftSyntaxHighlighter() }),
    Scala(listOf("scala", "sc"), { ScalaSyntaxHighlighter() }),
    Lua(listOf("lua"), { LuaSyntaxHighlighter() }),
    ObjectiveC(listOf("m", "mm"), { ObjectiveCSyntaxHighlighter() }),
    Bash(listOf("sh", "bash", "zsh"), { BashSyntaxHighlighter() }),
}