package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun remoteContextMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuElement> = listOf(
    ContextMenuElement.ContextTextEntry(
        label = "Edit",
        icon = { painterResource(AppIcons.EDIT) },
        onClick = onEdit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Delete",
        icon = { painterResource(AppIcons.DELETE) },
        onClick = onDelete
    ),
)