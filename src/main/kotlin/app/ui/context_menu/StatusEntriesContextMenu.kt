package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import app.git.StatusEntry
import app.git.StatusType

@OptIn(ExperimentalFoundationApi::class)
fun statusEntriesContextMenuItems(
    statusEntry: StatusEntry,
    entryType: EntryType,
    onReset: () -> Unit,
    onDelete: () -> Unit = {},
    onBlame: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf<ContextMenuItem>().apply {
        if (statusEntry.statusType != StatusType.ADDED) {
            add(
                ContextMenuItem(
                    label = "Reset",
                    onClick = onReset,
                )
            )

            if (statusEntry.statusType != StatusType.REMOVED) {
                add(
                    ContextMenuItem(
                        label = "Blame file",
                        onClick = onBlame,
                    )
                )
            }
        }

        if (
            entryType == EntryType.UNSTAGED &&
            statusEntry.statusType != StatusType.REMOVED
        ) {
            add(
                ContextMenuItem(
                    label = "Delete file",
                    onClick = onDelete,
                )
            )
        }
    }
}


enum class EntryType {
    STAGED,
    UNSTAGED,
}