package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement

@Composable
fun <T> SideMenuPanel(
    title: String,
    icon: Painter? = null,
    items: List<T>,
    isExpanded: Boolean = false,
    onExpand: () -> Unit,
    itemContent: @Composable (T) -> Unit,
    headerHoverIcon: @Composable (() -> Unit)? = null,
    contextItems: () -> List<ContextMenuElement> = { emptyList() },
) {
    VerticalExpandable(
        isExpanded = isExpanded,
        onExpand = onExpand,
        header = {
            ContextMenu(
                items = contextItems
            ) {
                SideMenuHeader(
                    text = title,
                    icon = icon,
                    itemsCount = items.count(),
                    hoverIcon = headerHoverIcon,
                    isExpanded = isExpanded,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            for (item in items) {
                itemContent(item)
            }
        }
    }
}