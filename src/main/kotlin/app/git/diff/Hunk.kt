package app.git.diff

data class Hunk(val header: String, val lines: List<Line>)

data class Line(val text: String, val oldLineNumber: Int, val newLineNumber: Int, val lineType: LineType) {
    // lines numbers are stored based on 0 being the first one but on a file the first line is the 1, so increment it!
    val displayOldLineNumber: Int = oldLineNumber + 1
    val displayNewLineNumber: Int = newLineNumber + 1
}

enum class LineType {
    CONTEXT,
    ADDED,
    REMOVED,
}