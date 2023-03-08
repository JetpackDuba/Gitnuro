package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons
import org.eclipse.jgit.diff.DiffEntry

@OptIn(ExperimentalFoundationApi::class)
fun commitedChangesEntriesContextMenuItems(
    diffEntry: DiffEntry,
    onBlame: () -> Unit,
    onHistory: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (diffEntry.changeType != DiffEntry.ChangeType.ADD ||
            diffEntry.changeType != DiffEntry.ChangeType.DELETE
        ) {
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
}