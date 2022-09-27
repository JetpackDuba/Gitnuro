package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import org.eclipse.jgit.diff.DiffEntry

@OptIn(ExperimentalFoundationApi::class)
fun commitedChangesEntriesContextMenuItems(
    diffEntry: DiffEntry,
    onBlame: () -> Unit,
    onHistory: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf<ContextMenuItem>().apply {
        if (diffEntry.changeType != DiffEntry.ChangeType.ADD ||
            diffEntry.changeType != DiffEntry.ChangeType.DELETE
        ) {
            add(
                ContextMenuItem(
                    label = "Blame file",
                    onClick = onBlame,
                )
            )
            add(
                ContextMenuItem(
                    label = "File history",
                    onClick = onHistory,
                )
            )
        }
    }
}