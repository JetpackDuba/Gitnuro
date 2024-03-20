package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.ui.resizePointerIconEast

@Composable
fun TripleVerticalSplitPanel(
    modifier: Modifier = Modifier,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    firstWidth: Float,
    thirdWidth: Float,
    onFirstSizeDrag: (Float) -> Unit,
    onThirdSizeDrag: (Float) -> Unit,
) {
    Row(
        modifier = modifier
    ) {
        if (firstWidth > 0) {
            Box(modifier = Modifier.width(firstWidth.dp)) {
                first()
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .draggable(
                        state = rememberDraggableState {
                            onFirstSizeDrag(it)
                        },
                        orientation = Orientation.Horizontal
                    )
                    .pointerHoverIcon(resizePointerIconEast)
            )
        }

        Box(Modifier.weight(1f, true)) {
            second()
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .draggable(
                    rememberDraggableState {
                        onThirdSizeDrag(it)
                    }, Orientation.Horizontal
                )
                .pointerHoverIcon(resizePointerIconEast)
        )

        Box(modifier = Modifier.width(thirdWidth.dp)) {
            third()
        }
    }
}