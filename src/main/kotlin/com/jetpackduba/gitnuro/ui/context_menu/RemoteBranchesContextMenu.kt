@file:OptIn(ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun remoteBranchesContextMenu(
    onDeleteBranch: () -> Unit
): List<ContextMenuElement> {
    return listOf(
        ContextMenuElement.ContextTextEntry(
            label = "Delete remote branch",
            icon = { painterResource(AppIcons.DELETE) },
            onClick = onDeleteBranch
        ),
    )
}