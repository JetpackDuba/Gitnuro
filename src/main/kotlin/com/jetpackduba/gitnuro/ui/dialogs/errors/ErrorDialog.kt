package com.jetpackduba.gitnuro.ui.dialogs.errors

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.onDoubleClick
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.copy
import com.jetpackduba.gitnuro.generated.resources.error
import com.jetpackduba.gitnuro.generated.resources.error_dialog_copy_button_tooltip
import com.jetpackduba.gitnuro.generated.resources.generic_button_ok
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.theme.secondarySurface
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ErrorDialog(
    error: Error,
    onAccept: () -> Unit,
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current
    var showStackTrace by remember { mutableStateOf(false) }

    MaterialDialog(
        onCloseRequested = onAccept,
    ) {
        Column(
            modifier = Modifier
                .width(580.dp)
        ) {
            Row {
                Text(
                    text = error.errorTitle(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground,
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painterResource(Res.drawable.error),
                    contentDescription = null,
                    tint = MaterialTheme.colors.error,
                    modifier = Modifier.size(24.dp)
                        .onDoubleClick {
                            showStackTrace = !showStackTrace
                        }
                )
            }

            SelectionContainer {
                Text(
                    text = error.exception.message.orEmpty(), // TODO
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .widthIn(max = 600.dp),
                    style = MaterialTheme.typography.body2,
                )
            }

            if (showStackTrace) {
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
                        text = stringResource(Res.string.error_dialog_copy_button_tooltip),
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
                                painter = painterResource(Res.drawable.copy),
                                contentDescription = null,
                                tint = MaterialTheme.colors.onSurface,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 32.dp)
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.generic_button_ok),
                    onClick = onAccept
                )
            }
        }
    }
}

fun copyMessageError(clipboard: ClipboardManager, ex: Exception) {
    clipboard.setText(AnnotatedString(ex.stackTraceToString()))
}
