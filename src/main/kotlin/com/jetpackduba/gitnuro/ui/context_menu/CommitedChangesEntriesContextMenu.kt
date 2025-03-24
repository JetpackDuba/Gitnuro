package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.blame
import com.jetpackduba.gitnuro.generated.resources.history
import org.eclipse.jgit.diff.DiffEntry
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
fun committedChangesEntriesContextMenuItems(
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
}