package com.jetpackduba.gitnuro.ui.log

import com.jetpackduba.gitnuro.git.graph.GraphNode2
import org.eclipse.jgit.lib.Ref

sealed interface LogDialog {
    data object None : LogDialog
    data class NewBranch(val graphNode: GraphNode2) : LogDialog
    data class NewTag(val graphNode: GraphNode2) : LogDialog
    data class ResetBranch(val graphNode: GraphNode2) : LogDialog
    data class ChangeDefaultBranch(val ref: Ref) : LogDialog
}