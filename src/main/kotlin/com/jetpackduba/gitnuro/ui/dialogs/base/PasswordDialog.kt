package com.jetpackduba.gitnuro.ui.dialogs.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordDialog(
    title: String,
    subtitle: String,
    icon: DrawableResource,
    password: String = "",
    cancelButtonText: String = stringResource(Res.string.generic_button_cancel),
    isRetry: Boolean = false,
    retryMessage: String = "",
    onDismiss: () -> Unit,
    onAccept: (password: String) -> Unit,
) {
    var showRetryMessage by remember(isRetry) { mutableStateOf(isRetry) }
    var showPassword by remember { mutableStateOf(false) }
    var passwordField by remember { mutableStateOf(password) }
    val passwordFieldFocusRequester = remember { FocusRequester() }

    SingleTextFieldDialog(
        icon = painterResource(icon),
        title = title,
        subtitle = subtitle,
        value = passwordField,
        onValueChange = {
            passwordField = it
            showRetryMessage = false
        },
        fieldFocusRequester = passwordFieldFocusRequester,
        isPrimaryActionEnabled = true,
        primaryActionText = stringResource(Res.string.generic_button_continue),
        cancelActionText = cancelButtonText,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        fieldTrailingIcon = {
            val visibilityIcon = if (showPassword) {
                Res.drawable.visibility_off
            } else {
                Res.drawable.visibility
            }

            IconButton(
                onClick = {
                    showPassword = !showPassword
                    passwordFieldFocusRequester.requestFocus()
                },
                modifier = Modifier.handOnHover()
                    .size(20.dp),
            ) {
                Icon(
                    painterResource(visibilityIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                )
            }
        },
        onDismiss = onDismiss,
        onPrimaryActionClicked = { onAccept(passwordField) },
    ) {
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
    }
}