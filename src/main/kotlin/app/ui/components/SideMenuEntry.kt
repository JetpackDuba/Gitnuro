package app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.headerBackground
import app.theme.primaryTextColor
import app.theme.secondaryTextColor

@Composable
fun SideMenuEntry(
    text: String,
    icon: Painter? = null,
    itemsCount: Int,
    hoverIcon: @Composable (() -> Unit)? = null,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .height(32.dp)
            .fillMaxWidth()
            .hoverable(hoverInteraction)
            .background(color = MaterialTheme.colors.headerBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primaryTextColor,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
            )
        }

        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
            maxLines = 1,
            fontSize = 14.sp,
            color = MaterialTheme.colors.primaryTextColor,
            overflow = TextOverflow.Ellipsis,
        )

        if (hoverIcon != null && isHovered) {
            hoverIcon()
        } else
            Text(
                text = itemsCount.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colors.secondaryTextColor,
                modifier = Modifier.padding(end = 8.dp),
            )
    }
}