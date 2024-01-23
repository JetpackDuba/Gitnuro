package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.backgroundIf
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.onDoubleClick
import com.jetpackduba.gitnuro.theme.backgroundSelected
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement

private const val TREE_START_PADDING = 12

@Composable
fun FileEntry(
    icon: ImageVector,
    iconColor: Color,
    parentDirectoryPath: String,
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: () -> List<ContextMenuElement>,
    trailingAction: (@Composable BoxScope.(isHovered: Boolean) -> Unit)?,
) {
    FileEntry(
        icon = rememberVectorPainter(icon),
        iconColor = iconColor,
        parentDirectoryPath = parentDirectoryPath,
        fileName = fileName,
        isSelected = isSelected,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        depth = depth,
        onGenerateContextMenu = onGenerateContextMenu,
        trailingAction = trailingAction
    )
}

@Composable
fun FileEntry(
    icon: Painter,
    iconColor: Color,
    parentDirectoryPath: String,
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: () -> List<ContextMenuElement>,
    trailingAction: (@Composable BoxScope.(isHovered: Boolean) -> Unit)?,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .handMouseClickable { onClick() }
            .onDoubleClick(onDoubleClick)
            .fillMaxWidth()
            .hoverable(hoverInteraction)
    ) {
        ContextMenu(
            items = {
                onGenerateContextMenu()
            },
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
                    .padding(start = (TREE_START_PADDING * depth).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp),
                    tint = iconColor,
                )

                if (parentDirectoryPath.isNotEmpty()) {
                    Text(
                        text = parentDirectoryPath.removeSuffix("/"),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )

                    Text(
                        text = "/",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Visible,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )
                }
                Text(
                    text = fileName,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }

        trailingAction?.invoke(this, isHovered)
    }
}

@Composable
fun DirectoryEntry(
    dirName: String,
    onClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: () -> List<ContextMenuElement>,
) {

    FileEntry(
        icon = painterResource(AppIcons.FOLDER),
        iconColor = MaterialTheme.colors.onBackground,
        isSelected = false,
        onClick = onClick,
        onDoubleClick = {},
        parentDirectoryPath = "",
        fileName = dirName,
        depth = depth,
        onGenerateContextMenu = onGenerateContextMenu,
        trailingAction = null,
    )
}