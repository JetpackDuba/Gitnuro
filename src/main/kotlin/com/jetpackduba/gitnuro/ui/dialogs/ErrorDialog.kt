package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.theme.secondarySurface
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import kotlinx.coroutines.delay

@Composable
fun ErrorDialog(
    error: Error,
    onAccept: () -> Unit,
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current

    MaterialDialog {
        Column(
            modifier = Modifier
                .width(580.dp)
        ) {
            Row {
                Text(
                    text = error.title ?: "Error",
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

            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .height(400.dp)
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = error.exception.stackTraceToString(),
                    onValueChange = {},
                    readOnly = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = MaterialTheme.colors.secondarySurface),
                    textStyle = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScroll)
                        .verticalScroll(verticalScroll),
                )

                HorizontalScrollbar(
                    rememberScrollbarAdapter(horizontalScroll),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )

                VerticalScrollbar(
                    rememberScrollbarAdapter(verticalScroll),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )

                InstantTooltip(
                    "Copy error",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                ) {
                    IconButton(
                        onClick = {
                            copyMessageError(clipboard, error.exception)
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .handOnHover()
                            .background(MaterialTheme.colors.background.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            painter = painterResource(AppIcons.COPY),
                            contentDescription = "Copy stacktrace",
                            tint = MaterialTheme.colors.onSurface,
                        )
                    }
                }
            }

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

fun copyMessageError(clipboard: ClipboardManager, ex: Exception) {
    clipboard.setText(AnnotatedString(ex.stackTraceToString()))
}
