package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import app.git.StatusEntry
import app.git.StatusType
import org.eclipse.jgit.diff.DiffEntry

@OptIn(ExperimentalFoundationApi::class)
fun stagedEntriesContextMenuItems(
    diffEntry: StatusEntry,
    onReset: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf<ContextMenuItem>().apply {
        if (diffEntry.statusType != StatusType.ADDED) {
            add(
                ContextMenuItem(
                    label = "Reset",
                    onClick = onReset,
                )
            )
        }
    }
}
