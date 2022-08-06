package app.ui.log

import app.git.graph.GraphNode

sealed class LogDialog {
    object None : LogDialog()
    data class NewBranch(val graphNode: GraphNode) : LogDialog()
    data class NewTag(val graphNode: GraphNode) : LogDialog()
    data class ResetBranch(val graphNode: GraphNode) : LogDialog()
}