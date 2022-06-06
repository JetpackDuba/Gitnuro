@file:OptIn(ExperimentalComposeUiApi::class)

package app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import app.theme.dialogOverlay

@Composable
fun MaterialDialog(
    alignment: Alignment = Alignment.Center,
    paddingHorizontal: Dp = 16.dp,
    paddingVertical: Dp = 16.dp,
    onCloseRequested: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Popup(
        focusable = true,
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset.Zero
        }
    ) {

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.dialogOverlay)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent {
                    if (it.key == Key.Escape) {
                        onCloseRequested()
                        true
                    } else
                        false
                },
            contentAlignment = alignment,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colors.background)
                    .padding(
                        horizontal = paddingHorizontal,
                        vertical = paddingVertical,
                    )
            ) {
                content()
            }
        }
    }
}