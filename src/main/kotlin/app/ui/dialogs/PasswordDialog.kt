package app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.keybindings.KeybindingOption
import app.keybindings.matchesBinding
import app.theme.outlinedTextFieldColors
import app.theme.primaryTextColor
import app.ui.components.PrimaryButton

@Composable
fun PasswordDialog(
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit
) {
    var passwordField by remember { mutableStateOf("") }
    val passwordFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog(onCloseRequested = onReject) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Text(
                text = "Introduce your default SSH key's password",
                modifier = Modifier
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colors.primaryTextColor,
            )
            OutlinedTextField(
                modifier = Modifier.padding(bottom = 8.dp)
                    .focusOrder(passwordFieldFocusRequester) {
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
                singleLine = true,
                label = {
                    Text(
                        "Password",
                        style = MaterialTheme.typography.body1.copy(MaterialTheme.colors.primaryVariant),
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
                    onClick = {
                        onReject()
                    }
                ) {
                    Text("Cancel")
                }
                PrimaryButton(
                    modifier = Modifier.focusOrder(buttonFieldFocusRequester) {
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