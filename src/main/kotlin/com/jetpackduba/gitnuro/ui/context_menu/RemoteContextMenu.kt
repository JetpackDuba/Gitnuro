package com.jetpackduba.gitnuro.ui.context_menu

fun remoteContextMenu(
    onEditRemotes: () -> Unit,
): List<ContextMenuElement> = listOf(
    ContextMenuElement.ContextTextEntry(
        label = "Edit remotes",
        onClick = onEditRemotes
    ),
)