package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class KotlinSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = listOf(
        "as",
        "as?",
        "break",
        "by",
        "catch",
        "class",
        "constructor",
        "continue",
        "do",
        "dynamic",
        "else",
        "false",
        "finally",
        "for",
        "fun",
        "if",
        "import",
        "in",
        "!in",
        "interface",
        "is",
        "!is",
        "null",
        "object",
        "package",
        "return",
        "super",
        "this",
        "throw",
        "true",
        "try",
        "val",
        "var",
        "when",
        "where",
        "while",
        "actual",
        "abstract",
        "annotation",
        "companion",
        "const",
        "crossinline",
        "data",
        "enum",
        "expect",
        "external",
        "final",
        "infix",
        "inline",
        "inner",
        "internal",
        "lateinit",
        "noinline",
        "open",
        "operator",
        "out",
        "override",
        "private",
        "protected",
        "public",
        "reified",
        "sealed",
        "suspend",
        "tailrec",
        "vararg",
    )

    override fun isAnnotation(word: String): Boolean = word.startsWith("@")
    override fun isComment(line: String): Boolean = line.startsWith("//")
}