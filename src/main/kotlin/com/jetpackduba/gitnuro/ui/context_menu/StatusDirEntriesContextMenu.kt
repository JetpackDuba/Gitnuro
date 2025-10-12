package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun statusDirEntriesContextMenuItems(
    entryType: EntryType,
    onStageChanges: () -> Unit,
    onDiscardDirectoryChanges: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        addContextMenu(
            composableLabel = {
                if (entryType == EntryType.STAGED) {
                    stringResource(Res.string.status_dir_entries_context_menu_unstage_changes)
                } else {
                    stringResource(Res.string.status_dir_entries_context_menu_stage_changes)
                }
            },
            icon = {
                if (entryType == EntryType.STAGED) {
                    painterResource(Res.drawable.remove_done)
                } else {
                    painterResource(Res.drawable.done)
                }
            },
            onClick = onStageChanges,
        )

        if (entryType == EntryType.UNSTAGED) {
            add(ContextMenuElement.ContextSeparator)

            addContextMenu(
                composableLabel = { stringResource(Res.string.status_dir_entries_context_menu_discard_changes) },
                icon = { painterResource(Res.drawable.undo) },
                onClick = onDiscardDirectoryChanges,
            )
        }
    }
}
