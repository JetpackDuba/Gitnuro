package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.delete
import com.jetpackduba.gitnuro.generated.resources.edit
import com.jetpackduba.gitnuro.generated.resources.remote_context_menu_edit
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun remoteContextMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuElement> = listOf(
    ContextMenuElement.ContextTextEntry(
        composableLabel = { stringResource(Res.string.remote_context_menu_edit) },
        icon = { painterResource(Res.drawable.edit) },
        onClick = onEdit
    ),
    ContextMenuElement.ContextTextEntry(
        composableLabel = { stringResource(Res.string.remote_context_menu_delete) },
        icon = { painterResource(Res.drawable.delete) },
        onClick = onDelete
    ),
)