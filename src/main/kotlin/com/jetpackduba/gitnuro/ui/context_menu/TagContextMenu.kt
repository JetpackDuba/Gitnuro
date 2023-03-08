package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun tagContextMenuItems(
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Checkout tag",
            icon = { painterResource(AppIcons.START) },
            onClick = onCheckoutTag
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Delete tag",
            icon = { painterResource(AppIcons.DELETE) },
            onClick = onDeleteTag
        )
    )
}