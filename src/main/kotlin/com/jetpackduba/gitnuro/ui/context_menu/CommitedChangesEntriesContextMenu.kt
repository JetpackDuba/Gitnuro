package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.*
import org.eclipse.jgit.diff.DiffEntry
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun committedChangesEntriesContextMenuItems(
    diffEntry: DiffEntry,
    onBlame: () -> Unit,
    onHistory: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (diffEntry.changeType != DiffEntry.ChangeType.ADD ||
            diffEntry.changeType != DiffEntry.ChangeType.DELETE
        ) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.committed_changes_context_menu_blame_file) },
                icon = { painterResource(Res.drawable.blame) },
                onClick = onBlame,
            )
            addContextMenu(
                composableLabel = { stringResource(Res.string.committed_changes_context_menu_file_history) },
                icon = { painterResource(Res.drawable.history) },
                onClick = onHistory,
            )
        }
    }
}