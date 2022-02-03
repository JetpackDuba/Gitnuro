package app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.headerBackground
import app.theme.primaryTextColor
import app.theme.secondaryTextColor

@Composable
fun SideMenuEntry(
    text: String,
    icon: Painter? = null,
    itemsCount: Int,
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.headerBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if(icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primaryTextColor,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
            )
        }

        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
            maxLines = 1,
            fontSize = 14.sp,
            color = MaterialTheme.colors.primaryTextColor,
            overflow = TextOverflow.Ellipsis,
        )


        Text(
            text = itemsCount.toString(),
            fontSize = 14.sp,
            color = MaterialTheme.colors.secondaryTextColor,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}