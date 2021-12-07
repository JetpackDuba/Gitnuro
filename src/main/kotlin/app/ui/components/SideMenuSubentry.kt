@file:OptIn(ExperimentalComposeUiApi::class)

package app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.primaryTextColor

@Composable
fun SideMenuSubentry(
    text: String,
    iconResourcePath: String,
    bold: Boolean = false,
    onClick: () -> Unit = {},
    additionalInfo: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconResourcePath),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(16.dp),
            tint = MaterialTheme.colors.primary,
        )

        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = true),
            maxLines = 1,
            fontSize = 14.sp,
            fontWeight = if(bold) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colors.primaryTextColor,
            overflow = TextOverflow.Ellipsis,
        )

        additionalInfo()
    }
}