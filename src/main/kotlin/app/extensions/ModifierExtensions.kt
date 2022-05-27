package app.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon

fun Modifier.backgroundIf(condition: Boolean, color: Color): Modifier {
    return if (condition) {
        this.background(color)
    } else {
        this
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.handMouseClickable(onClick: () -> Unit): Modifier {
    return this.clickable { onClick() }
        .pointerHoverIcon(PointerIconDefaults.Hand)
}