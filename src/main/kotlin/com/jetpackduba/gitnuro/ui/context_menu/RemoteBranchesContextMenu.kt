@file:OptIn(ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.res.painterResource

fun remoteBranchesContextMenu(
    onDeleteBranch: () -> Unit
): List<ContextMenuElement> {
    return listOf(
        ContextMenuElement.ContextTextEntry(
            label = "Delete remote branch",
            icon = { painterResource("delete.svg") },
            onClick = onDeleteBranch
        ),
    )
}