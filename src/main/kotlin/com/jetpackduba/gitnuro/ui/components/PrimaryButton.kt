package com.jetpackduba.gitnuro.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.backgroundIf
import com.jetpackduba.gitnuro.extensions.handMouseClickable

@Preview
@Composable
fun PrimaryButtonPreview() {
    PrimaryButton(
        text = "Primary button",
        onClick = {},
    )
}

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    focusable: Boolean = true,
    backgroundColor: Color = MaterialTheme.colors.primary,
    backgroundDisabled: Color = MaterialTheme.colors.primary.copy(0.5f),
    textColor: Color = MaterialTheme.colors.onPrimary,
    disabledTextColor: Color = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .backgroundIf(enabled, backgroundColor, backgroundDisabled)
            .focusable(focusable)
            .run {
                if (enabled) {
                    handMouseClickable {
                        onClick()
                    }
                } else
                    this
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (enabled) textColor else disabledTextColor
        )
    }
}
