package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.res.painterResource

fun tagContextMenuItems(
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Checkout tag",
            icon = { painterResource("start.svg") },
            onClick = onCheckoutTag
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Delete tag",
            icon = { painterResource("delete.svg") },
            onClick = onDeleteTag
        )
    )
}