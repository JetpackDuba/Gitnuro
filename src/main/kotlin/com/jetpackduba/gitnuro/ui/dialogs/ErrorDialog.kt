package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.ui.components.PrimaryButton

@Composable
fun ErrorDialog(
    error: Error,
    onAccept: () -> Unit,
) {
    MaterialDialog {
        Column(
            modifier = Modifier
                .width(580.dp)
        ) {
            Row {
                Text(
                    text = "Error",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground,
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painterResource(AppIcons.ERROR),
                    contentDescription = null,
                    tint = MaterialTheme.colors.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = error.message,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .widthIn(max = 600.dp),
                style = MaterialTheme.typography.body2,
            )

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 32.dp)
            ) {
                PrimaryButton(
                    text = "OK",
                    onClick = onAccept
                )
            }
        }
    }
}