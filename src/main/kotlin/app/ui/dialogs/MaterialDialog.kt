package app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

@Composable
fun MaterialDialog(
    alignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit
) {
    Popup(
        focusable = true,
        popupPositionProvider = object: PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset.Zero
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black, Color.Black)), alpha = 0.8f),
            contentAlignment = alignment,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colors.background)
                    .padding(all = 16.dp)
            ) {
                content()
            }
        }
    }
}