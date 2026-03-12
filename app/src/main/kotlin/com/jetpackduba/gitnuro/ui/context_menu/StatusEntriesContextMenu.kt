package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.EntryType
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun statusEntryContextMenuItems(
    statusEntry: StatusEntry,
    entryType: EntryType,
    onReset: () -> Unit,
    onDelete: () -> Unit = {},
    onBlame: () -> Unit,
    onHistory: () -> Unit,
    onCopyFilePath: (relative: Boolean) -> Unit,
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

        addContextMenu(
            composableLabel = { stringResource(Res.string.status_entries_context_menu_open_file_in_folder) },
            icon = { painterResource(Res.drawable.folder_open) },
            onClick = onOpenFileInFolder,
        )


        add(ContextMenuElement.ContextSeparator)

        addContextMenu(
            composableLabel = {
                stringResource(Res.string.status_entries_context_menu_copy_file_absolute_path)
            },
            icon = { painterResource(Res.drawable.copy) },
            onClick = { onCopyFilePath(false) },
        )

        addContextMenu(
            composableLabel = {
                stringResource(Res.string.status_entries_context_menu_copy_file_relative_path)
            },
            icon = { painterResource(Res.drawable.copy_all) },
            onClick = { onCopyFilePath(true) },
        )
    }
}

fun statusEntriesContextMenuItems(
    selectedEntriesCount: Int,
    entryType: EntryType,
    onDiscard: () -> Unit,
    onStageSelected: () -> Unit,
    onUnstageSelected: () -> Unit,
    onCopyFilesPath: (relative: Boolean) -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {

        when (entryType) {
            EntryType.STAGED -> addContextMenu(
                composableLabel = {
                    stringResource(
                        Res.string.status_entries_context_menu_unstage_multiple_files,
                        selectedEntriesCount,
                    )
                },
                icon = { painterResource(Res.drawable.remove_done) },
                onClick = onUnstageSelected,
            )

            EntryType.UNSTAGED -> addContextMenu(
                composableLabel = {
                    stringResource(
                        Res.string.status_entries_context_menu_stage_multiple_files,
                        selectedEntriesCount,
                    )
                },
                icon = { painterResource(Res.drawable.done) },
                onClick = onStageSelected,
            )
        }

        addContextMenu(
            composableLabel = {
                stringResource(
                    Res.string.status_entries_context_menu_discard_multiple_files,
                    selectedEntriesCount
                )
            },
            icon = { painterResource(Res.drawable.undo) },
            onClick = onDiscard,
        )

        add(ContextMenuElement.ContextSeparator)

        addContextMenu(
            composableLabel = {
                stringResource(Res.string.status_entries_context_menu_copy_files_absolute_paths)
            },
            icon = { painterResource(Res.drawable.copy) },
            onClick = { onCopyFilesPath(false) },
        )

        addContextMenu(
            composableLabel = {
                stringResource(Res.string.status_entries_context_menu_copy_files_relative_paths)
            },
            icon = { painterResource(Res.drawable.copy_all) },
            onClick = { onCopyFilesPath(true) },
        )
    }
}
