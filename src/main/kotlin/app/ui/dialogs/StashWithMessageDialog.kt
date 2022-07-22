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
import app.keybindings.KeybindingOption
import app.keybindings.matchesBinding
import app.theme.outlinedTextFieldColors
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.ui.components.PrimaryButton

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StashWithMessageDialog(
    onReject: () -> Unit,
    onAccept: (stashMessage: String) -> Unit
) {
    var textField by remember { mutableStateOf("") }
    val textFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog(onCloseRequested = onReject) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .focusOrder(textFieldFocusRequester) {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) && textField.isNotBlank()) {
                            onAccept(textField)
                            true
                        } else {
                            false
                        }
                    },
                value = textField,
                label = {
                    Text(
                        "New stash message",
                        style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.primaryVariant),
                    )
                },
                textStyle = MaterialTheme.typography.body1,
                colors = outlinedTextFieldColors(),
                onValueChange = {
                    textField = it
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
                    modifier = Modifier.focusOrder(buttonFieldFocusRequester) {
                        this.previous = textFieldFocusRequester
                        this.next = textFieldFocusRequester
                    },
                    enabled = textField.isNotBlank(),
                    onClick = {
                        onAccept(textField)
                    },
                    text = "Stash"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }
}