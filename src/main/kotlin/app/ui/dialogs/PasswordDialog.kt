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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PasswordDialog(
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit
) {
    var passwordField by remember { mutableStateOf("") }
    val passwordFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog {
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
            )
            OutlinedTextField(
                modifier = Modifier.padding(bottom = 8.dp)
                    .focusOrder(passwordFieldFocusRequester) {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent {
                        if (it.key == Key.Enter) {
                            onAccept(passwordField)
                            true
                        } else {
                            false
                        }
                    },
                value = passwordField,
                singleLine = true,
                label = { Text("Password", fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 14.sp),
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
                Button(
                    modifier = Modifier.focusOrder(buttonFieldFocusRequester) {
                        this.previous = passwordFieldFocusRequester
                    },
                    onClick = {
                        onAccept(passwordField)
                    }
                ) {
                    Text("Ok")
                }
            }

        }

        LaunchedEffect(Unit) {
            passwordFieldFocusRequester.requestFocus()
        }
    }
}