package com.jetpackduba.gitnuro.extensions

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.coroutineScope

fun Modifier.backgroundIf(condition: Boolean, color: Color, elseColor: Color? = null): Modifier {
    return if (condition) {
        this.background(color)
    } else if (elseColor != null) {
        this.background(elseColor)
    } else
        this
}

fun Modifier.handMouseClickable(onClick: () -> Unit): Modifier {
    return this
        .clickable { onClick() }
        .handOnHover()
}

fun Modifier.handMouseClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    onClick: () -> Unit
): Modifier {
    return this
        .clickable(
            interactionSource = interactionSource,
            indication = indication,
        ) { onClick() }
        .handOnHover()
}

/**
 * Ignore keyboard events of that components.
 * Specially useful for clickable components that may get focused and become clickable when pressing ENTER.
 */
fun Modifier.ignoreKeyEvents(): Modifier {
    return this.onPreviewKeyEvent { true }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.handOnHover(): Modifier {
    return this.pointerHoverIcon(PointerIcon.Hand)
}

/**
 * Detects double clicks without messing with other [clickable] features (like hover effect, single-click duration and
 * accessibility features). Doesn't work when combined with long press though. This is a simplified version of
 * [PointerInputScope.detectTapGestures] for double clicks only, and without consuming the first click.
 */
@Composable
fun Modifier.onDoubleClick(
    onDoubleClick: () -> Unit,
): Modifier {
    return this.pointerInput(Unit) {
        coroutineScope {
            awaitEachGesture {
                // Detect first click without consuming it (other, independent handlers want it).
                awaitFirstDown()
                val firstUp = waitForUpOrCancellation() ?: return@awaitEachGesture

                // Detect and consume the second click if it's received within the timeout.
                val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
                    var change: PointerInputChange
                    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
                    do {
                        change = awaitFirstDown()
                    } while (change.uptimeMillis < minUptime)
                    change
                } ?: return@awaitEachGesture
                secondDown.consume()
                val secondUp = waitForUpOrCancellation() ?: return@awaitEachGesture
                secondUp.consume()

                // Both clicks happened in time, fire the event.
                onDoubleClick()
            }
        }
    }
}

// TODO Try to restore hover that was shown with clickable modifier
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.fastClickable(key: Any = Unit, key2: Any = Unit, onClick: () -> Unit) =
    this.handOnHover()
        .hoverBackground()
        .pointerInput(key, key2) {
            while (true) {
                val lastMouseEvent = awaitPointerEventScope { awaitFirstDownEvent() }
                val mouseEvent = lastMouseEvent.awtEventOrNull

                if (mouseEvent != null) {
                    if (lastMouseEvent.button.isPrimary) {
                        onClick()
                    }
                }
            }
        }

@Composable
private fun Modifier.hoverBackground(): Modifier {
    val hoverInteraction = remember { MutableInteractionSource() }

    return this.hoverable(hoverInteraction)
        .indication(hoverInteraction, LocalIndication.current)
}
