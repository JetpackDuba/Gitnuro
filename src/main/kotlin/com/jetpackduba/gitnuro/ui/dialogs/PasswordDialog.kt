package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton

@Composable
fun PasswordDialog(
    title: String,
    subtitle: String,
    icon: String,
    password: String = "",
    cancelButtonText: String = "Cancel",
    isRetry: Boolean = false,
    retryMessage: String = "",
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
) {
    var showRetryMessage by remember(isRetry) { mutableStateOf(isRetry) }
    var showPassword by remember { mutableStateOf(false) }
    var passwordField by remember { mutableStateOf(password) }
    val passwordFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog(onCloseRequested = onReject) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(IntrinsicSize.Min)
        ) {

            Icon(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .padding(vertical = 16.dp),
                tint = MaterialTheme.colors.onBackground,
            )

            Text(
                text = title,
                modifier = Modifier
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
            )

            Text(
                text = subtitle,
                modifier = Modifier
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            AdjustableOutlinedTextField(
                modifier = Modifier
                    .focusRequester(passwordFieldFocusRequester)
                    .focusProperties {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT)) {
                            onAccept(passwordField)
                            true
                        } else {
                            false
                        }
                    },
                value = passwordField,
                maxLines = 1,
                colors = outlinedTextFieldColors(),
                onValueChange = {
                    passwordField = it
                    showRetryMessage = false
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val visibilityIcon = if (showPassword) {
                        "visibility_off.svg"
                    } else {
                        "visibility.svg"
                    }

                    IconButton(
                        onClick = { showPassword = !showPassword },
                        modifier = Modifier.handOnHover()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    // return focus to field instead so the user can continue typing without having to click on the field again
                                    passwordFieldFocusRequester.requestFocus()
                                }
                            },
                    ) {
                        Icon(
                            painterResource(visibilityIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onBackground,
                        )
                    }
                }
            )

            if (showRetryMessage) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.error)
                        .align(Alignment.CenterHorizontally)
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        retryMessage,
                        color = MaterialTheme.colors.onError,
                    )
                }

            }

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                PrimaryButton(
                    text = cancelButtonText,
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onReject,
                    backgroundColor = Color.Transparent,
                    textColor = MaterialTheme.colors.onBackground,
                )
                PrimaryButton(
                    modifier = Modifier
                        .focusRequester(buttonFieldFocusRequester)
                        .focusProperties {
                            this.previous = passwordFieldFocusRequester
                        },
                    onClick = {
                        onAccept(passwordField)
                    },
                    text = "Continue"
                )
            }

        }

        LaunchedEffect(Unit) {
            passwordFieldFocusRequester.requestFocus()
        }
    }
}