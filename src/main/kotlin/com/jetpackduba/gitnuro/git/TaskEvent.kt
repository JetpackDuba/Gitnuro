package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.ui.SelectedItem
import org.eclipse.jgit.revwalk.RevCommit

sealed interface TaskEvent {
    data class RebaseInteractive(val revCommit: RevCommit) : TaskEvent
    data class ScrollToGraphItem(val selectedItem: SelectedItem) : TaskEvent
}