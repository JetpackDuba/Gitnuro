package app.ui.components

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import app.maxSidePanelHeight

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SideMenuPanel(
    title: String,
    icon: Painter? = null,
    items: List<T>,
    isExpanded: Boolean = false,
    onExpand: () -> Unit,
    itemContent: @Composable (T) -> Unit,
    headerHoverIcon: @Composable (() -> Unit)? = null,
    contextItems: () -> List<ContextMenuItem> = { emptyList() },
) {
    VerticalExpandable(
        isExpanded = isExpanded,
        onExpand = onExpand,
        header = {
            ContextMenuArea(
                items = contextItems
            ) {
                SideMenuEntry(
                    text = title,
                    icon = icon,
                    itemsCount = items.count(),
                    hoverIcon = headerHoverIcon
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.background)
        ) {
            for (item in items) {
                itemContent(item)
            }
        }
    }
}