package com.jetpackduba.gitnuro.ui.components.tooltip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.theme.isDark
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DelayedTooltip(text: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TooltipArea(
        modifier = modifier,
        tooltip = {
            if (text != null) {
                val border = if (MaterialTheme.colors.isDark) {
                    BorderStroke(2.dp, MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.2f))
                } else
                    null

                Card(
                    backgroundColor = MaterialTheme.colors.background,
                    border = border,
                    elevation = 10.dp,
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
    ) {
        content()
    }
}