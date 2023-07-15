package com.jetpackduba.gitnuro.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable

@Preview
@Composable
fun SwitchPreview() {
    AppSwitch(false, {})
}

@Composable
fun AppSwitch(
    isChecked: Boolean,
    onValueChanged: (Boolean) -> Unit,
) {
    val background: Color
    val startPadding: Dp
    val uncheckedColor = Color(
        red = (MaterialTheme.colors.onBackground.red + MaterialTheme.colors.primary.red) / 2,
        green = (MaterialTheme.colors.onBackground.green + MaterialTheme.colors.primary.green) / 2,
        blue = (MaterialTheme.colors.onBackground.blue + MaterialTheme.colors.primary.blue) / 2,
        alpha = 0.4f
    )


    if (isChecked) {
        background = MaterialTheme.colors.primary
        startPadding = 24.dp
    } else {
        background = uncheckedColor
        startPadding = 4.dp
    }

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .handMouseClickable { onValueChanged(!isChecked) }
            .padding(top = 4.dp, bottom = 4.dp, end = 4.dp, start = startPadding),
        contentAlignment = if (isChecked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.onSecondary)
        )
    }
}
