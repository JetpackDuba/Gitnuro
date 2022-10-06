package com.jetpackduba.gitnuro.ui.components

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color


@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.colors.onPrimary,
    disabledTextColor: Color = MaterialTheme.colors.onBackground,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = textColor
        ),
    ) {
        Text(
            text,
            color = if(enabled) textColor else disabledTextColor
        )
    }
}