package com.jetpackduba.gitnuro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.theme.tertiarySurface

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = MaterialTheme.colors.onPrimary,
    backgroundButton: Color = MaterialTheme.colors.primary,
    maxLines: Int = 1,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundButton)
            .handMouseClickable { onClick() },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = textColor,
            maxLines = maxLines,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
        )
    }
}

@Composable
fun SecondaryButtonCompactable(
    modifier: Modifier = Modifier,
    icon: String,
    text: String,
    onBackgroundColor: Color = MaterialTheme.colors.onPrimary,
    backgroundButton: Color,
    maxLines: Int = 1,
    isParentHovered: Boolean,
    onClick: () -> Unit,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered, isParentHovered) {
        isExpanded = when {
            isHovered -> true
            isExpanded && isParentHovered -> true
            else -> false
        }
    }

    val targetBackground: Color
    val iconColor: Color
    val iconPadding: Float

    if (isExpanded) {
        targetBackground = backgroundButton
        iconColor = onBackgroundColor
        iconPadding = 12f
    } else {
        targetBackground = MaterialTheme.colors.tertiarySurface
        iconColor = MaterialTheme.colors.onBackground
        iconPadding = 0f
    }

    val backgroundColorState by animateColorAsState(targetBackground)
    val iconColorState by animateColorAsState(iconColor)
    val iconPaddingState by animateFloatAsState(iconPadding)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColorState)
            .hoverable(hoverInteraction)
            .handMouseClickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = iconColorState,
            modifier = Modifier
                .padding(start = iconPaddingState.dp, end = 8.dp)
                .size(18.dp)
        )

        AnimatedVisibility(
            visible = isExpanded,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                color = onBackgroundColor,
                maxLines = maxLines,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = 12.dp)
            )
        }
    }
}