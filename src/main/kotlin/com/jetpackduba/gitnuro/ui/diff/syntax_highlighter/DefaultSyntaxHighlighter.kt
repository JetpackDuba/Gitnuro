package com.jetpackduba.gitnuro.ui.diff.syntax_highlighter

class DefaultSyntaxHighlighter : SyntaxHighlighter() {
    override fun loadKeywords(): List<String> = emptyList()
    override fun isAnnotation(word: String): Boolean = false
    override fun isComment(line: String): Boolean = false
}