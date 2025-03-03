package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.blame
import com.jetpackduba.gitnuro.generated.resources.history
import com.jetpackduba.gitnuro.generated.resources.undo
import org.jetbrains.compose.resources.painterResource
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
                    icon = { painterResource(Res.drawable.undo) },
                    onClick = onReset,
                )
            )

            if (statusEntry.statusType != StatusType.REMOVED) {
                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "Blame file",
                        icon = { painterResource(Res.drawable.blame) },
                        onClick = onBlame,
                    )
                )

                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "File history",
                        icon = { painterResource(Res.drawable.history) },
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
                    icon = { painterResource(Res.drawable.delete) },
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