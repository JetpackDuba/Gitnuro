package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class ScalaSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = listOf(
        "abstract",
        "case",
        "catch",
        "class",
        "def",
        "do",
        "else",
        "extends",
        "false",
        "final",
        "finally",
        "for",
        "forSome",
        "if",
        "implicit",
        "import",
        "lazy",
        "match",
        "new",
        "null",
        "object",
        "override",
        "package",
        "private",
        "protected",
        "return",
        "sealed",
        "super",
        "this",
        "throw",
        "trait",
        "true",
        "try",
        "type",
        "val",
        "var",
        "while",
        "with",
        "yield",
    )

    override fun isAnnotation(word: String): Boolean = word.startsWith("@")
    override fun isComment(line: String): Boolean = line.startsWith("//")
}