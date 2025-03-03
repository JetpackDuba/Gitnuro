package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.done
import com.jetpackduba.gitnuro.generated.resources.remove_done
import com.jetpackduba.gitnuro.generated.resources.undo
import org.jetbrains.compose.resources.painterResource

fun statusDirEntriesContextMenuItems(
    entryType: EntryType,
    onStageChanges: () -> Unit,
    onDiscardDirectoryChanges: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {

        val (text, icon) = if (entryType == EntryType.STAGED) {
            "Unstage changes in the directory" to Res.drawable.remove_done
        } else {
            "Stage changes in the directory" to Res.drawable.done
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
                    icon = { painterResource(Res.drawable.undo) },
                    onClick = onDiscardDirectoryChanges,
                )
            )
        }
    }
}
