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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable

import com.jetpackduba.gitnuro.theme.onBackgroundSecondary

@Composable
fun SideMenuHeader(
    text: String,
    icon: Painter? = null,
    itemsCount: Int,
    isExpanded: Boolean,
    onExpand: () -> Unit = {},
    hoverIcon: @Composable (() -> Unit)? = null,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
            .hoverable(hoverInteraction)
            .handMouseClickable { onExpand() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (isExpanded) "expand_more.svg" else "chevron_right.svg"),
            contentDescription = null,
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(16.dp),
        )

        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(16.dp),
            )
        }

        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
            maxLines = 1,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackground,
            overflow = TextOverflow.Ellipsis,
        )

        if (hoverIcon != null && isHovered) {
            hoverIcon()
        } else
            Text(
                text = itemsCount.toString(),
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.padding(end = 16.dp),
            )
    }
}