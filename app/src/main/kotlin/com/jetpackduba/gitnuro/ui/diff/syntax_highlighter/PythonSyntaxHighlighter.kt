package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class PythonSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = listOf(
        "False",
        "await",
        "else",
        "import",
        "pass",
        "None",
        "break",
        "except",
        "in",
        "raise",
        "True",
        "class",
        "finally",
        "is",
        "return",
        "and",
        "continue",
        "for",
        "lambda",
        "try",
        "as",
        "def",
        "from",
        "nonlocal",
        "while",
        "assert",
        "del",
        "global",
        "not",
        "with",
        "async",
        "elif",
        "if",
        "or",
        "yield",
    )

    override fun isAnnotation(word: String): Boolean = word.startsWith("@")
    override fun isComment(line: String): Boolean = line.startsWith("//")
}
