package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun stashesContextMenuItems(
    onApply: () -> Unit,
    onPop: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuElement> {
    return listOf(
        ContextMenuElement.ContextTextEntry(
            label = "Apply stash",
            icon = { painterResource(AppIcons.APPLY_STASH) },
            onClick = onApply
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Pop stash",
            icon = { painterResource(AppIcons.APPLY_STASH) },
            onClick = onPop
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Drop stash",
            icon = { painterResource(AppIcons.DELETE) },
            onClick = onDelete
        ),
    )
}