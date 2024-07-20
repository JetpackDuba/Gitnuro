package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.ui.resizePointerIconEast

const val MAX_SIDE_PANE_PROPORTION = 0.4f

@Composable
fun TripleVerticalSplitPanel(
    modifier: Modifier = Modifier,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    firstWidth: Float,
    thirdWidth: Float,
    onFirstSizeDragStarted: (Float) -> Unit,
    onFirstSizeChange: (Float) -> Unit,
    onFirstSizeDragStopped: () -> Unit,
    onThirdSizeDragStarted: (Float) -> Unit,
    onThirdSizeChange: (Float) -> Unit,
    onThirdSizeDragStopped: () -> Unit,
) {
    val density = LocalDensity.current.density
    var screenWidth by remember { mutableStateOf(0) }
    val screenWidthInDp = remember(screenWidth) { (screenWidth / density) }

    val firstWidthLimited = remember(firstWidth, screenWidthInDp) {
        calcLimitedWidth(screenWidthInDp, firstWidth)
    }

    val thirdWidthLimited = remember(thirdWidth, screenWidthInDp) {
        calcLimitedWidth(screenWidthInDp, thirdWidth)
    }

    Row(
        modifier = modifier
            .onSizeChanged {
                screenWidth = it.width
            }
    ) {
        if (firstWidth > 0) {
            Box(
                modifier = Modifier
                    .width(firstWidthLimited.dp)
            ) {
                first()
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .draggable(
                        state = rememberDraggableState {
                            onFirstSizeChange(it)
                        },
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            onFirstSizeDragStarted(firstWidthLimited)
                        },
                        onDragStopped = {
                            onFirstSizeDragStopped()
                        },
                    )
                    .pointerHoverIcon(resizePointerIconEast)
            )
        }

        Box(
            Modifier
                .weight(1f, true)
        ) {
            second()
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .draggable(
                    state = rememberDraggableState {
                        onThirdSizeChange(it)
                    },
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        onThirdSizeDragStarted(thirdWidthLimited)
                    },
                    onDragStopped = {
                        onThirdSizeDragStopped()
                    },
                )
                .pointerHoverIcon(resizePointerIconEast)
        )

        Box(
            modifier = Modifier
                .width(thirdWidthLimited.dp)
        ) {
            third()
        }
    }
}

private fun calcLimitedWidth(maxSize: Float, currentSize: Float): Float {
    return if (currentSize > maxSize * MAX_SIDE_PANE_PROPORTION) {
        maxSize * MAX_SIDE_PANE_PROPORTION
    } else {
        currentSize
    }
}