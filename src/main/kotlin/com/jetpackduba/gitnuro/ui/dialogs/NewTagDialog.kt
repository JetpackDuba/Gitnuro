package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.tag
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton

@Composable
fun NewTagDialog(
    onReject: () -> Unit,
    onAccept: (tagName: String) -> Unit
) {
    var field by remember { mutableStateOf("") }
    val fieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog(onCloseRequested = onReject) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painterResource(Res.drawable.tag),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp),
                tint = MaterialTheme.colors.onBackground,
            )

            Text(
                text = "Set tag name",
                modifier = Modifier
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "Create a new tag on the specified commit",
                modifier = Modifier
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            AdjustableOutlinedTextField(
                modifier = Modifier
                    .focusRequester(fieldFocusRequester)
                    .focusProperties {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) && field.isNotBlank()) {
                            onAccept(field)
                            true
                        } else {
                            false
                        }
                    },
                value = field,
                maxLines = 1,
                onValueChange = {
                    field = it
                },
            )
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                PrimaryButton(
                    text = "Cancel",
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onReject,
                    backgroundColor = Color.Transparent,
                    textColor = MaterialTheme.colors.onBackground,
                )
                PrimaryButton(
                    modifier = Modifier
                        .focusRequester(buttonFieldFocusRequester)
                        .focusProperties {
                            this.previous = fieldFocusRequester
                            this.next = fieldFocusRequester
                        },
                    enabled = field.isNotBlank(),
                    onClick = {
                        onAccept(field)
                    },
                    text = "Create tag"
                )
            }
        }

        LaunchedEffect(Unit) {
            fieldFocusRequester.requestFocus()
        }
    }
}