package com.jetpackduba.gitnuro.ui.dialogs

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
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.PrimaryButton

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
                    .focusRequester(textFieldFocusRequester)
                    .focusProperties {
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
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.body1,
                    )
                }
                PrimaryButton(
                    modifier = Modifier
                        .focusRequester(buttonFieldFocusRequester)
                        .focusProperties {
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