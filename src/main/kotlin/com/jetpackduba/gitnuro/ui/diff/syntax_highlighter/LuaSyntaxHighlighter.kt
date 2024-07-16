package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class LuaSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = listOf(
        "and",
        "break",
        "do",
        "else",
        "elseif",
        "end",
        "false",
        "for",
        "function",
        "if",
        "in",
        "local",
        "nil",
        "not",
        "or",
        "repeat",
        "return",
        "then",
        "true",
        "until",
        "while"
    )

    override fun isAnnotation(word: String): Boolean = false
    override fun isComment(line: String): Boolean = line.startsWith("--")
}