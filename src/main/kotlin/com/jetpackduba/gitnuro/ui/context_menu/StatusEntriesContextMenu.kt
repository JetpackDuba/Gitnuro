package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun statusEntriesContextMenuItems(
    statusEntry: StatusEntry,
    entryType: EntryType,
    onReset: () -> Unit,
    onDelete: () -> Unit = {},
    onBlame: () -> Unit,
    onHistory: () -> Unit,
    onOpenFileInFolder: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (statusEntry.statusType != StatusType.ADDED) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.status_entries_context_menu_discard_file_changes) },
                icon = { painterResource(Res.drawable.undo) },
                onClick = onReset,
            )

            if (statusEntry.statusType != StatusType.REMOVED) {
                addContextMenu(
                    composableLabel = { stringResource(Res.string.status_entries_context_menu_blame_file) },
                    icon = { painterResource(Res.drawable.blame) },
                    onClick = onBlame,
                )

                addContextMenu(
                    composableLabel = { stringResource(Res.string.status_entries_context_menu_file_history) },
                    icon = { painterResource(Res.drawable.history) },
                    onClick = onHistory,
                )
            }
        }

        if (
            entryType == EntryType.UNSTAGED &&
            statusEntry.statusType != StatusType.REMOVED
        ) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.status_entries_context_menu_delete_file) },
                icon = { painterResource(Res.drawable.delete) },
                onClick = onDelete,
            )
        }

        add(
            ContextMenuElement.ContextTextEntry(
                label = "Open file in folder",
                icon = { painterResource(Res.drawable.folder_open) },
                onClick = onOpenFileInFolder,
            )
        )
    }
}


enum class EntryType {
    STAGED,
    UNSTAGED,
}