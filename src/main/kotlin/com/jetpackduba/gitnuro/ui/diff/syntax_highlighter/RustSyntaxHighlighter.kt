package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class RustSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = listOf(
        "as",
        "async",
        "await",
        "break",
        "const",
        "continue",
        "crate",
        "dyn",
        "else",
        "enum",
        "extern",
        "false",
        "fn",
        "for",
        "if",
        "impl",
        "in",
        "let",
        "loop",
        "match",
        "mod",
        "move",
        "mut",
        "pub",
        "ref",
        "return",
        "Self",
        "self",
        "static",
        "struct",
        "super",
        "trait",
        "true",
        "type",
        "union",
        "unsafe",
        "use",
        "where",
        "while",
    )

    override fun isAnnotation(word: String): Boolean = word.startsWith("#[") && word.endsWith("]")

    override fun isComment(line: String): Boolean = line.startsWith("//")
}
