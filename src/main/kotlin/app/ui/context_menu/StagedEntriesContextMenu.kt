package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import org.eclipse.jgit.diff.DiffEntry

@OptIn(ExperimentalFoundationApi::class)
fun stagedEntriesContextMenuItems(
    diffEntry: DiffEntry,
    onReset: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf<ContextMenuItem>().apply {
        if (diffEntry.changeType != DiffEntry.ChangeType.ADD) {
            add(
                ContextMenuItem(
                    label = "Reset",
                    onClick = onReset,
                )
            )
        }
    }
}
