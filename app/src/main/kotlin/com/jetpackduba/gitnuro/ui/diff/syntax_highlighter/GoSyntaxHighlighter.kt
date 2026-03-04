package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class GoSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = listOf(
        "break",
        "default",
        "func",
        "interface",
        "select",
        "case",
        "defer",
        "go",
        "map",
        "struct",
        "chan",
        "else",
        "goto",
        "package",
        "switch",
        "const",
        "fallthrough",
        "if",
        "range",
        "type",
        "continue",
        "for",
        "import",
        "return",
        "var",
    )

    override fun isAnnotation(word: String): Boolean = false
    override fun isComment(line: String): Boolean = line.startsWith("//")
}