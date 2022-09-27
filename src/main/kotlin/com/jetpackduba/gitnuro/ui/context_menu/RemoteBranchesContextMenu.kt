@file:OptIn(ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi

fun remoteBranchesContextMenu(
    onDeleteBranch: () -> Unit
): List<ContextMenuItem> {
    return mutableListOf(
        ContextMenuItem(
            label = "Delete remote branch",
            onClick = onDeleteBranch
        ),
    )
}