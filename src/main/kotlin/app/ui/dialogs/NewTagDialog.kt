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
import app.theme.outlinedTextFieldColors
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.ui.components.PrimaryButton

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewTagDialog(
    onReject: () -> Unit,
    onAccept: (tagName: String) -> Unit
) {
    var tagField by remember { mutableStateOf("") }
    val tagFieldFocusRequester = remember { FocusRequester() }
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
                    .focusOrder(tagFieldFocusRequester) {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent {
                        if (it.key == Key.Enter && tagField.isBlank()) {
                            onAccept(tagField)
                            true
                        } else {
                            false
                        }
                    },
                value = tagField,
                singleLine = true,
                label = { Text("New tag name", fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
                colors = outlinedTextFieldColors(),
                onValueChange = {
                    tagField = it
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
                        this.previous = tagFieldFocusRequester
                        this.next = tagFieldFocusRequester
                    },
                    enabled = tagField.isNotBlank(),
                    onClick = {
                        onAccept(tagField)
                    },
                    text = "Create tag",
                )
            }
        }

        LaunchedEffect(Unit) {
            tagFieldFocusRequester.requestFocus()
        }
    }
}