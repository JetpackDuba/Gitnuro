package com.jetpackduba.gitnuro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.git.ProcessingState
import com.jetpackduba.gitnuro.ui.components.PrimaryButton

@Composable
fun ProcessingScreen(
    processingState: ProcessingState.Processing,
    onCancelOnGoingTask: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
            .onPreviewKeyEvent { true } // Disable all keyboard events
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (processingState.title.isNotEmpty()) {
                Text(
                    processingState.title,
                    style = MaterialTheme.typography.h3,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (processingState.subtitle.isNotEmpty()) {
                Text(
                    processingState.subtitle,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(bottom = 32.dp),
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.width(280.dp)
                    .padding(bottom = 32.dp),
                color = MaterialTheme.colors.secondary,
            )

            if (processingState.isCancellable) {
                PrimaryButton(
                    text = "Cancel",
                    onClick = onCancelOnGoingTask,
                )
            }
        }
    }
}