package app.ui.dialogs

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
import androidx.compose.ui.unit.dp
import app.keybindings.KeybindingOption
import app.keybindings.matchesBinding
import app.theme.outlinedTextFieldColors
import app.theme.textButtonColors
import app.ui.components.PrimaryButton

@Composable
fun NewBranchDialog(
    onReject: () -> Unit,
    onAccept: (branchName: String) -> Unit
) {
    var branchField by remember { mutableStateOf("") }
    val branchFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog(onCloseRequested = onReject) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(branchFieldFocusRequester)
                    .focusProperties {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) && branchField.isNotBlank()) {
                            onAccept(branchField)
                            true
                        } else {
                            false
                        }
                    },
                value = branchField,
                singleLine = true,
                label = {
                    Text(
                        "New branch name",
                        style = MaterialTheme.typography.body1.copy(MaterialTheme.colors.primaryVariant),
                    )
                },
                textStyle = MaterialTheme.typography.body1,
                colors = outlinedTextFieldColors(),
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
                            this.previous = branchFieldFocusRequester
                            this.next = branchFieldFocusRequester
                        },
                    enabled = branchField.isNotBlank(),
                    onClick = {
                        onAccept(branchField)
                    },
                    text = "Create branch"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        branchFieldFocusRequester.requestFocus()
    }
}