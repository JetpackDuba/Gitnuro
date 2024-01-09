package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.ui.resizePointerIconEast

@Composable
fun TripleVerticalSplit(
    modifier: Modifier = Modifier,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    initialFirstWidth: Float,
    minFirstWidth: Float,
    initialThirdWidth: Float,
    minThirdWidth: Float,
    onFirstSizeChanged: (Float) -> Unit,
    onThirdSizeChanged: (Float) -> Unit,
) {
    val density = LocalDensity.current.density
    var firstWidth by remember { mutableStateOf(initialFirstWidth) }
    var thirdWidth by remember { mutableStateOf(initialThirdWidth) }

    Row(
        modifier = modifier
    ) {
        Box(modifier = Modifier.width((firstWidth).dp)) {
            first()
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .draggable(
                    state = rememberDraggableState {
                        val newWidth = firstWidth + it / density

                        if (newWidth > minFirstWidth) {
                            firstWidth = newWidth
                            onFirstSizeChanged(firstWidth)
                        }
                    },
                    orientation = Orientation.Horizontal
                )
                .pointerHoverIcon(resizePointerIconEast)
        )

        Box(Modifier.weight(1f, true)) {
            second()
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .draggable(
                    rememberDraggableState {
                        val newWidth = thirdWidth - it / density

                        if(newWidth > minThirdWidth) {
                            thirdWidth = newWidth
                            onThirdSizeChanged(thirdWidth)
                        }
                    }, Orientation.Horizontal
                )
                .pointerHoverIcon(resizePointerIconEast)
        )

        Box(modifier = Modifier.width(thirdWidth.dp)) {
            third()
        }
    }
}