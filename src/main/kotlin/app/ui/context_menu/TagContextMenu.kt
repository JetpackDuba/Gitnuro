package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun tagContextMenuItems(
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf(
        ContextMenuItem(
            label = "Checkout tag",
            onClick = onCheckoutTag
        ),
        ContextMenuItem(
            label = "Delete tag",
            onClick = onDeleteTag
        )
    )
}