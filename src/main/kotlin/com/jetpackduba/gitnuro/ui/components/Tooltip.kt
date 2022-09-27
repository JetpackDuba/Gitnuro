package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Card(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 10.dp,
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
    ) {
        content()
    }
}