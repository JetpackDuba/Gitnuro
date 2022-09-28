package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource

fun stashesContextMenuItems(
    onApply: () -> Unit,
    onPop: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuElement> {
    return listOf(
        ContextMenuElement.ContextTextEntry(
            label = "Apply stash",
            icon = { painterResource("apply_stash.svg") },
            onClick = onApply
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Pop stash",
            icon = { painterResource("apply_stash.svg") },
            onClick = onPop
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Drop stash",
            icon = { painterResource("delete.svg") },
            onClick = onDelete
        ),
    )
}