package app.extensions

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

fun Modifier.backgroundIf(condition: Boolean, color: Color): Modifier {
    return if (condition) {
        this.background(color)
    } else {
        this
    }
}