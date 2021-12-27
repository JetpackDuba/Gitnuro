package app.git.diff

data class Hunk(val header: String, val lines: List<Line>)

sealed class Line(val content: String) {
    class ContextLine(content: String, val oldLineNumber: Int, val newLineNumber: Int): Line(content)
    class AddedLine(content: String, val oldLineNumber: Int, val newLineNumber: Int): Line(content)
    class RemovedLine(content: String, val lineNumber: Int): Line(content)
}