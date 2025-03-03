package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.delete
import com.jetpackduba.gitnuro.generated.resources.edit
import org.jetbrains.compose.resources.painterResource

fun remoteContextMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuElement> = listOf(
    ContextMenuElement.ContextTextEntry(
        label = "Edit",
        icon = { painterResource(Res.drawable.edit) },
        onClick = onEdit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Delete",
        icon = { painterResource(Res.drawable.delete) },
        onClick = onDelete
    ),
)