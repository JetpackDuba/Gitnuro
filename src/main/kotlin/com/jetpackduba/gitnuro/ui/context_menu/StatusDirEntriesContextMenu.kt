package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun statusDirEntriesContextMenuItems(
    entryType: EntryType,
    onStageChanges: () -> Unit,
    onDiscardDirectoryChanges: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {

        val (text, icon) = if (entryType == EntryType.STAGED) {
            "Unstage changes in the directory" to AppIcons.REMOVE_DONE
        } else {
            "Stage changes in the directory" to AppIcons.DONE
        }

        add(
            ContextMenuElement.ContextTextEntry(
                label = text,
                icon = { painterResource(icon) },
                onClick = onStageChanges,
            )
        )


        if (entryType == EntryType.UNSTAGED) {
            add(ContextMenuElement.ContextSeparator)

            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Discard changes in the directory",
                    icon = { painterResource(AppIcons.UNDO) },
                    onClick = onDiscardDirectoryChanges,
                )
            )
        }
    }
}
