package com.jetpackduba.gitnuro.ui.components.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.AnnotatedString
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
    InstantTooltip(
        text = text?.let { AnnotatedString(it) },
        trailingContent = trailingContent,
        modifier = modifier,
        position = position,
        enabled = enabled,
        content = content,
    )
}

@Composable
fun InstantTooltip(
    text: AnnotatedString?,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    position: InstantTooltipPosition = InstantTooltipPosition.BOTTOM,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val isHovered by hoverInteractionSource.collectIsHoveredAsState()
    val tooltipHoverInteractionSource = remember { MutableInteractionSource() }
    val isTooltipHovered by tooltipHoverInteractionSource.collectIsHoveredAsState()
    val (coordinates, setCoordinates) = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val (cursorCoordinatesOnTooltipHover, setCursorCoordinatesOnTooltipHover) = remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .hoverable(hoverInteractionSource)
            .onGloballyPositioned {
                setCoordinates(it)
            },
    ) {
        content()
    }

    val isHoveringTooltipAndContent = remember(cursorCoordinatesOnTooltipHover, coordinates) {
        if (cursorCoordinatesOnTooltipHover != null && coordinates != null) {
            val positionInWindowX = coordinates.positionInWindow().x
            val positionInWindowY = coordinates.positionInWindow().y
            val isXInContent =
                cursorCoordinatesOnTooltipHover.x in positionInWindowX..positionInWindowX + coordinates.size.width
            val isYInContent =
                cursorCoordinatesOnTooltipHover.y in positionInWindowY..positionInWindowY + coordinates.size.height
            isXInContent && isYInContent
        } else {
            false
        }
    }

    LaunchedEffect(isTooltipHovered) {
        println("Is tooltipHovered: $isTooltipHovered")
    }

    if ((isHovered || isHoveringTooltipAndContent) && coordinates != null && text != null && enabled) {
        val popupPosition = remember { mutableStateOf(Offset.Zero) }
        Popup(
            properties = PopupProperties(
                focusable = false,
            ),
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    val position = popupPosition(coordinates, position, popupContentSize)

                    return position
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
                    .onGloballyPositioned {
                        popupPosition.value = it.positionInWindow()
                    }
                    .pointerInput(popupPosition) {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Move) {
                                val cursorPosition = event.changes.getOrNull(0)?.position

                                if (cursorPosition != null) {
                                    setCursorCoordinatesOnTooltipHover(popupPosition.value + cursorPosition)
                                }
                            } else if (event.type == PointerEventType.Exit) {
                                setCursorCoordinatesOnTooltipHover(null)
                            }
                        }
                    }
                    .padding(padding)
                    .shadow(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colors.background)
                    .width(IntrinsicSize.Max)
                    .hoverable(tooltipHoverInteractionSource)
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

private fun popupPosition(
    coordinates: LayoutCoordinates,
    position: InstantTooltipPosition,
    popupContentSize: IntSize
): IntOffset {
    val positionInWindow = coordinates.positionInWindow()
    val contentSize = coordinates.size

    val x = getXBasedOnTooltipPosition(position, positionInWindow, contentSize, popupContentSize) //
    val y = getYBasedOnTooltipPosition(position, positionInWindow, contentSize, popupContentSize)

    return IntOffset(x, y)
}

fun getXBasedOnTooltipPosition(
    position: InstantTooltipPosition,
    positionInWindow: Offset,
    contentSize: IntSize,
    popupContentSize: IntSize,
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
    popupContentSize: IntSize,
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