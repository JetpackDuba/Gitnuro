package app.git.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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

    Dialog(
        onCloseRequest = onReject,
        title = "",

        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Introduce your remote server credentials")

            OutlinedTextField(
                modifier = Modifier.focusOrder(userFieldFocusRequester) {
                    this.next = passwordFieldFocusRequester
                },
                value = userField,
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
                    },
                value = passwordField,
                label = { Text("Password", fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 14.sp),
                onValueChange = {
                    passwordField = it
                },
                visualTransformation = PasswordVisualTransformation()
            )
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
}
