package app.ui.log

import app.git.graph.GraphNode
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

sealed class LogDialog {
    object None : LogDialog()
    data class NewBranch(val graphNode: GraphNode) : LogDialog()
    data class NewTag(val graphNode: GraphNode) : LogDialog()
    data class ResetBranch(val graphNode: GraphNode) : LogDialog()
    data class MergeBranch(val ref: Ref) : LogDialog()
    data class RebaseBranch(val ref: Ref) : LogDialog()
}