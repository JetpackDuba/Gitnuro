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
    MaterialDialog {
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
            )

            OutlinedTextField(
                modifier = Modifier
                    .focusOrder(userFieldFocusRequester) {
                        this.next = passwordFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent {
                        if (it.key == Key.Enter) {
                            acceptDialog()
                            true
                        } else {
                            false
                        }
                    },
                value = userField,
                singleLine = true,
                label = { Text("User", fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 14.sp),
                onValueChange = {
                    userField = it
                },
            )
            OutlinedTextField(
                modifier = Modifier.padding(bottom = 8.dp)
                    .focusOrder(passwordFieldFocusRequester) {
                        this.previous = userFieldFocusRequester
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent {
                        if (it.key == Key.Enter) {
                            acceptDialog()
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
                        this.next = userFieldFocusRequester
                    },
                    onClick = {
                        onAccept(userField, passwordField)
                    }
                ) {
                    Text("Ok")
                }
            }
        }

        LaunchedEffect(Unit) {
            userFieldFocusRequester.requestFocus()
        }
    }
}