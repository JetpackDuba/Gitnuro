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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewBranchDialog(
    onReject: () -> Unit,
    onAccept: (branchName: String) -> Unit
) {
    var branchField by remember { mutableStateOf("") }
    val branchFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .focusOrder(branchFieldFocusRequester) {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent {
                        if (it.key == Key.Enter) {
                            onAccept(branchField)
                            true
                        } else {
                            false
                        }
                    },
                value = branchField,
                singleLine = true,
                label = { Text("New branch name", fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 14.sp),
                onValueChange = {
                    branchField = it
                },
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
                        this.previous = branchFieldFocusRequester
                        this.next = branchFieldFocusRequester
                    },
                    enabled = branchField.isNotEmpty(),
                    onClick = {
                        onAccept(branchField)
                    }
                ) {
                    Text("Create branch")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        branchFieldFocusRequester.requestFocus()
    }
}