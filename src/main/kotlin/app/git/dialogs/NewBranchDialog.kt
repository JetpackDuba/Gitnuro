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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState

@Composable
fun NewBranchDialog(
    onReject: () -> Unit,
    onAccept: (branchName: String) -> Unit
) {
    var branchField by remember { mutableStateOf("") }
    val userFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    Dialog(
        state = rememberDialogState(width = 0.dp, height = 0.dp),
        onCloseRequest = onReject,
        title = "",
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Enter a branch name")

            OutlinedTextField(
                modifier = Modifier.focusOrder(userFieldFocusRequester) {
                    this.next = buttonFieldFocusRequester
                },
                value = branchField,
                label = { Text("User", fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 14.sp),
                onValueChange = {
                    branchField = it
                },
            )
            Button(
                modifier = Modifier.focusOrder(buttonFieldFocusRequester) {
                    this.previous = userFieldFocusRequester
                    this.next = userFieldFocusRequester
                },
                onClick = {
                    onAccept(branchField)
                }
            ) {
                Text("Create branch")
            }
        }
    }
}