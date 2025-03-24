@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.dialogOverlay

@Composable
fun MaterialDialog(
    alignment: Alignment = Alignment.Center,
    paddingHorizontal: Dp = 16.dp,
    paddingVertical: Dp = 16.dp,
    background: Color = MaterialTheme.colors.surface,
    onCloseRequested: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Popup(
        properties = PopupProperties(
            focusable = true,
        ),
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
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
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.EXIT) && keyEvent.type == KeyEventType.KeyDown) {
                        onCloseRequested()
                        true
                    } else
                        false
                },
            contentAlignment = alignment,
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(background)
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