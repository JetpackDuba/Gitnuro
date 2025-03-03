package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.delete
import com.jetpackduba.gitnuro.generated.resources.start
import org.jetbrains.compose.resources.painterResource

fun tagContextMenuItems(
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Checkout tag's commit",
            icon = { painterResource(Res.drawable.start) },
            onClick = onCheckoutTag
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Delete tag",
            icon = { painterResource(Res.drawable.delete) },
            onClick = onDeleteTag
        )
    )
}