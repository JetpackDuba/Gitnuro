package com.jetpackduba.gitnuro.extensions

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.*

fun Modifier.backgroundIf(condition: Boolean, color: Color, elseColor: Color? = null): Modifier {
    return if (condition) {
        this.background(color)
    } else if(elseColor != null) {
        this.background(elseColor)
    } else
        this
}

fun Modifier.handMouseClickable(onClick: () -> Unit): Modifier {
    return this
        .clickable { onClick() }
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
    return this.pointerHoverIcon(PointerIconDefaults.Hand)
}

// TODO Try to restore hover that was shown with clickable modifier
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.fastClickable(key: Any = Unit, onClick: () -> Unit) =
    this.handOnHover()
        .hoverBackground()
        .pointerInput(key) {
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
