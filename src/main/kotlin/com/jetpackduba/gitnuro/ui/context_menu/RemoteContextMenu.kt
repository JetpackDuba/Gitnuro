package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun remoteContextMenu(
    onEditRemotes: () -> Unit,
) = listOf(
    ContextMenuItem(
        label = "Edit remotes",
        onClick = onEditRemotes
    ),
)