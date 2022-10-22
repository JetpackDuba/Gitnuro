package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType

fun statusEntriesContextMenuItems(
    statusEntry: StatusEntry,
    entryType: EntryType,
    onReset: () -> Unit,
    onDelete: () -> Unit = {},
    onBlame: () -> Unit,
    onHistory: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (statusEntry.statusType != StatusType.ADDED) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Discard file changes",
                    icon = { painterResource("undo.svg") },
                    onClick = onReset,
                )
            )

            if (statusEntry.statusType != StatusType.REMOVED) {
                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "Blame file",
                        icon = { painterResource("blame.svg") },
                        onClick = onBlame,
                    )
                )

                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "File history",
                        icon = { painterResource("history.svg") },
                        onClick = onHistory,
                    )
                )
            }
        }

        if (
            entryType == EntryType.UNSTAGED &&
            statusEntry.statusType != StatusType.REMOVED
        ) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Delete file",
                    icon = { painterResource("delete.svg") },
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