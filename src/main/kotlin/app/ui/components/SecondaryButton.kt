package app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.extensions.handMouseClickable

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = Color.White,
    backgroundButton: Color,
    maxLines: Int = 1,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundButton)
            .handMouseClickable { onClick() },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = textColor,
            maxLines = maxLines,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
        )
    }
}