package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun stashesContextMenuItems(
    onApply: () -> Unit,
    onPop: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf(
        ContextMenuItem(
            label = "Apply stash",
            onClick = onApply
        ),
        ContextMenuItem(
            label = "Pop stash",
            onClick = onPop
        ),
        ContextMenuItem(
            label = "Drop stash",
            onClick = onDelete
        ),
    )
}