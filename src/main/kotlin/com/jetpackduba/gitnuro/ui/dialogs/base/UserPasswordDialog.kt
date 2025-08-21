package com.jetpackduba.gitnuro.ui.dialogs.base

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserPasswordDialog(
    title: String,
    subtitle: String,
    icon: Painter,
    onDismiss: () -> Unit,
    onAccept: (user: String, password: String) -> Unit,
) {
    var userField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val userFieldFocusRequester = remember { FocusRequester() }
    val passwordFieldFocusRequester = remember { FocusRequester() }
    val actionsFocusRequester = remember { FocusRequester() }
    val acceptDialog = {
        onAccept(userField, passwordField)
    }

    IconBasedDialog(
        icon = icon,
        title = title,
        subtitle = subtitle,
        primaryActionText = stringResource(Res.string.generic_button_continue),
        onDismiss = onDismiss,
        onPrimaryActionClicked = acceptDialog,
        beforeActionsFocusRequester = passwordFieldFocusRequester,
        actionsFocusRequester = actionsFocusRequester,
        afterActionsFocusRequester = userFieldFocusRequester,
    ) {
        AdjustableOutlinedTextField(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .focusRequester(userFieldFocusRequester)
                .focusProperties {
                    this.next = passwordFieldFocusRequester
                }
                .width(300.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT)) {
                        passwordFieldFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                },
            value = userField,
            colors = outlinedTextFieldColors(),
            maxLines = 1,
            singleLine = true,
            hint = "Username",
            onValueChange = {
                userField = it
            },
        )

        AdjustableOutlinedTextField(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .focusRequester(passwordFieldFocusRequester)
                .focusProperties {
                    this.previous = userFieldFocusRequester
                    this.next = actionsFocusRequester
                }
                .width(300.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT)) {
                        acceptDialog()
                        true
                    } else {
                        false
                    }
                },
            value = passwordField,
            maxLines = 1,
            singleLine = true,
            colors = outlinedTextFieldColors(),
            hint = "Password",
            onValueChange = {
                passwordField = it
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
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
            }
        )

        LaunchedEffect(Unit) {
            userFieldFocusRequester.requestFocus()
        }
    }
}