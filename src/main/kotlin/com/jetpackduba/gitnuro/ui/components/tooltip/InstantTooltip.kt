package com.jetpackduba.gitnuro.ui.components.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.jetpackduba.gitnuro.theme.isDark

@Composable
fun InstantTooltip(
    text: String?,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    position: InstantTooltipPosition = InstantTooltipPosition.BOTTOM,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val isHovered by hoverInteractionSource.collectIsHoveredAsState()
    val (coordinates, setCoordinates) = remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .hoverable(hoverInteractionSource)
            .onGloballyPositioned {
                setCoordinates(it)
            },
    ) {
        content()
    }

    if (isHovered && coordinates != null && text != null && enabled) {
        Popup(
            properties = PopupProperties(
                focusable = false,
            ),
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val positionInWindow = coordinates.positionInWindow()
                    val contentSize = coordinates.size

                    val x = getXBasedOnTooltipPosition(position, positionInWindow, contentSize, popupContentSize) //
                    val y = getYBasedOnTooltipPosition(position, positionInWindow, contentSize, popupContentSize)

                    return IntOffset(x, y)
                }
            },
            onDismissRequest = {}
        ) {

            val padding = when (position) {
                InstantTooltipPosition.TOP -> PaddingValues(bottom = 4.dp)
                InstantTooltipPosition.BOTTOM -> PaddingValues(top = 4.dp)
                InstantTooltipPosition.LEFT -> PaddingValues(end = 4.dp)
                InstantTooltipPosition.RIGHT -> PaddingValues(start = 4.dp)
            }

            Row(
                modifier = Modifier
                    .padding(padding)
                    .shadow(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colors.background)
                    .width(IntrinsicSize.Max)
                    .run {
                        if (MaterialTheme.colors.isDark) {
                            this.border(
                                2.dp,
                                MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            )
                        } else
                            this
                    }
                    .padding(8.dp),
            ) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = MaterialTheme.colors.onBackground
                )

                if (trailingContent != null) {
                    Spacer(Modifier.width(8.dp))

                    trailingContent()
                }
            }
        }
    }
}

fun getXBasedOnTooltipPosition(
    position: InstantTooltipPosition,
    positionInWindow: Offset,
    contentSize: IntSize,
    popupContentSize: IntSize
): Int {
    return when (position) {
        InstantTooltipPosition.TOP, InstantTooltipPosition.BOTTOM -> (positionInWindow.x + (contentSize.width / 2)) - (popupContentSize.width / 2)
        InstantTooltipPosition.LEFT -> positionInWindow.x - popupContentSize.width
        InstantTooltipPosition.RIGHT -> positionInWindow.x + contentSize.width
    }.toInt()
}

fun getYBasedOnTooltipPosition(
    position: InstantTooltipPosition,
    positionInWindow: Offset,
    contentSize: IntSize,
    popupContentSize: IntSize
): Int {
    return when (position) {
        InstantTooltipPosition.TOP -> positionInWindow.y - popupContentSize.height
        InstantTooltipPosition.BOTTOM -> positionInWindow.y + contentSize.height
        InstantTooltipPosition.LEFT, InstantTooltipPosition.RIGHT -> (positionInWindow.y + (contentSize.height / 2)) - (popupContentSize.height / 2)
    }.toInt()
}

enum class InstantTooltipPosition {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}