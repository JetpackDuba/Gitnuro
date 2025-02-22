package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton

@Composable
fun UserPasswordDialog(
    title: String = "Introduce your remote server credentials",
    subtitle: String = "Your remote requires authentication with a\nusername and a password",
    icon: Painter = painterResource(AppIcons.LOCK),
    onReject: () -> Unit,
    onAccept: (user: String, password: String) -> Unit,
) {
    var userField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val userFieldFocusRequester = remember { FocusRequester() }
    val passwordFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }
    val acceptDialog = {
        onAccept(userField, passwordField)
    }
    MaterialDialog(
        onCloseRequested = onReject
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp),
                tint = MaterialTheme.colors.onBackground,
            )

            Text(
                text = title,
                modifier = Modifier
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = subtitle,
                modifier = Modifier
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

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
                        this.next = buttonFieldFocusRequester
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
                        AppIcons.VISIBILITY_OFF
                    } else {
                        AppIcons.VISIBILITY
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

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                PrimaryButton(
                    text = "Cancel",
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
                            this.next = userFieldFocusRequester
                        },
                    onClick = {
                        onAccept(userField, passwordField)
                    },
                    text = "Continue"
                )
            }
        }

        LaunchedEffect(Unit) {
            userFieldFocusRequester.requestFocus()
        }
    }
}