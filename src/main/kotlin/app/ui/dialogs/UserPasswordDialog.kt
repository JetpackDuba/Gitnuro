package app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.keybindings.KeybindingOption
import app.keybindings.matchesBinding
import app.theme.outlinedTextFieldColors
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.ui.components.PrimaryButton

@Composable
fun UserPasswordDialog(
    onReject: () -> Unit,
    onAccept: (user: String, password: String) -> Unit
) {
    var userField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }
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
            modifier = Modifier
                .background(MaterialTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Text(
                text = "Introduce your remote server credentials",
                modifier = Modifier
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colors.primaryTextColor,
            )

            OutlinedTextField(
                modifier = Modifier
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
                singleLine = true,
                colors = outlinedTextFieldColors(),
                label = {
                    Text(
                        "User",
                        style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.primaryVariant),
                    )
                },
                textStyle = MaterialTheme.typography.body1,
                onValueChange = {
                    userField = it
                },
            )
            OutlinedTextField(
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
                singleLine = true,
                label = {
                    Text(
                        "Password",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primaryVariant,
                    )
                },
                textStyle = MaterialTheme.typography.body1,
                colors = outlinedTextFieldColors(),
                onValueChange = {
                    passwordField = it
                },
                visualTransformation = PasswordVisualTransformation()
            )

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                TextButton(
                    modifier = Modifier.padding(end = 8.dp),
                    colors = textButtonColors(),
                    onClick = {
                        onReject()
                    }
                ) {
                    Text("Cancel")
                }
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