package com.jetpackduba.gitnuro.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.error
import com.jetpackduba.gitnuro.generated.resources.info
import com.jetpackduba.gitnuro.generated.resources.warning
import com.jetpackduba.gitnuro.models.Notification
import com.jetpackduba.gitnuro.models.NotificationType
import com.jetpackduba.gitnuro.theme.AppTheme
import org.jetbrains.compose.resources.painterResource


@Preview
@Composable
fun NotificationSuccessPreview() {
    AppTheme(customTheme = null) {
        Notification(NotificationType.Positive, "Hello world!")
    }
}

@Composable
fun Notification(notification: Notification) {
    val notificationShape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .padding(8.dp)
            .border(2.dp, MaterialTheme.colors.onBackground.copy(0.2f), notificationShape)
            .clip(notificationShape)
            .background(MaterialTheme.colors.background)
            .height(IntrinsicSize.Max)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val backgroundColor = when (notification.type) {
            NotificationType.Positive -> MaterialTheme.colors.primary
            NotificationType.Warning -> MaterialTheme.colors.secondary
            NotificationType.Error -> MaterialTheme.colors.error
        }

        val contentColor = when (notification.type) {
            NotificationType.Positive -> MaterialTheme.colors.onPrimary
            NotificationType.Warning -> MaterialTheme.colors.onSecondary
            NotificationType.Error -> MaterialTheme.colors.onError
        }

        val icon = when (notification.type) {
            NotificationType.Positive -> Res.drawable.info
            NotificationType.Warning -> Res.drawable.warning
            NotificationType.Error -> Res.drawable.error
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .background(backgroundColor)
                .fillMaxHeight()
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .padding(4.dp)
            )
        }

        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = notification.text,
                modifier = Modifier,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
            )
        }
    }
}