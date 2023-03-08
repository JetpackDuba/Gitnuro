package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons
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
                    icon = { painterResource(AppIcons.UNDO) },
                    onClick = onReset,
                )
            )

            if (statusEntry.statusType != StatusType.REMOVED) {
                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "Blame file",
                        icon = { painterResource(AppIcons.BLAME) },
                        onClick = onBlame,
                    )
                )

                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "File history",
                        icon = { painterResource(AppIcons.HISTORY) },
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
                    icon = { painterResource(AppIcons.DELETE) },
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