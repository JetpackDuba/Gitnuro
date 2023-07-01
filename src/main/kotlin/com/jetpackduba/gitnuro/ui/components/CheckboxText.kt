package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable

@Composable
fun CheckboxText(
    value: Boolean,
    onCheckedChange: () -> Unit,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.handMouseClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onCheckedChange,
        )
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier
                .padding(all = 8.dp)
                .size(12.dp)
        )

        Text(
            text,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackground,
        )
    }
}