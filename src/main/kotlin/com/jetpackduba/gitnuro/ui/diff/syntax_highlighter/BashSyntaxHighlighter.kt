package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class BashSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = listOf(
        "case",
        "coproc",
        "do",
        "done",
        "elif",
        "else",
        "esac",
        "fi",
        "for",
        "function",
        "if",
        "in",
        "select",
        "then",
        "until",
        "while",
        "time",
        "[[",
        "]]"
    )

    override fun isAnnotation(word: String): Boolean = false
    override fun isComment(line: String): Boolean = line.startsWith("#")
}