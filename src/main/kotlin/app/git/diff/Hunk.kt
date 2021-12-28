package app.git.diff

data class Hunk(val header: String, val lines: List<Line>)

data class Line(val text: String, val oldLineNumber: Int, val newLineNumber: Int, val lineType: LineType)

enum class LineType {
    CONTEXT,
    ADDED,
    REMOVED,
}