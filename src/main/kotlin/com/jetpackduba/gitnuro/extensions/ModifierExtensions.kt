package com.jetpackduba.gitnuro.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon

fun Modifier.backgroundIf(condition: Boolean, color: Color): Modifier {
    return if (condition) {
        this.background(color)
    } else {
        this
    }
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