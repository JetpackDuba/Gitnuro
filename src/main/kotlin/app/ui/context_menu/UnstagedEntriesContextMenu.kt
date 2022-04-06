package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import app.git.StatusEntry
import app.git.StatusType
import org.eclipse.jgit.diff.DiffEntry

@OptIn(ExperimentalFoundationApi::class)
fun unstagedEntriesContextMenuItems(
    statusEntry: StatusEntry,
    onReset: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf<ContextMenuItem>().apply {
        if (statusEntry.statusType != StatusType.ADDED) {
            add(
                ContextMenuItem(
                    label = "Reset",
                    onClick = onReset,
                )
            )
        }

        if (statusEntry.statusType != StatusType.REMOVED) {
            add(
                ContextMenuItem(
                    label = "Delete file",
                    onClick = onDelete,
                )
            )
        }
    }
}
