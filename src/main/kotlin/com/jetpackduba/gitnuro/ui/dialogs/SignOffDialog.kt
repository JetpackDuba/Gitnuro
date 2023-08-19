package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.viewmodels.SignOffDialogViewModel
import com.jetpackduba.gitnuro.viewmodels.SignOffState

@Composable
fun SignOffDialog(
    viewModel: SignOffDialogViewModel,
    onClose: () -> Unit,
) {
    val state = viewModel.state.collectAsState().value

    LaunchedEffect(viewModel) {
        viewModel.loadSignOffFormat()
    }


    var signOffField by remember(viewModel, state) {
        val signOff = if (state is SignOffState.Loaded) {
            state.signOffConfig.format
        } else {
            ""
        }

        mutableStateOf(TextFieldValue(signOff, TextRange(signOff.count())))
    }

    var enabledSignOff by remember(viewModel, state) {
        val signOff = if (state is SignOffState.Loaded) {
            state.signOffConfig.isEnabled
        } else {
            true
        }

        mutableStateOf(signOff)
    }

    val signOffFieldFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    MaterialDialog(onCloseRequested = onClose) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(IntrinsicSize.Min),
        ) {
            Icon(
                painterResource(AppIcons.SIGN),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp),
                tint = MaterialTheme.colors.onBackground,
            )

            Text(
                text = "Edit sign off",
                modifier = Modifier
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "Enable or disable the signoff or adjust its format",
                modifier = Modifier
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            AdjustableOutlinedTextField(
                modifier = Modifier
                    .focusRequester(signOffFieldFocusRequester)
                    .focusProperties {
                        this.next = buttonFieldFocusRequester
                    }
                    .width(300.dp),
                value = signOffField,
                enabled = state is SignOffState.Loaded,
                maxLines = 1,
                onValueChange = {
                    signOffField = it
                },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.handMouseClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (state is SignOffState.Loaded) {
                        enabledSignOff = !enabledSignOff
                    }
                }
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Checkbox(
                    checked = enabledSignOff,
                    enabled = state is SignOffState.Loaded,
                    onCheckedChange = {
                        enabledSignOff = it
                    },
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .size(12.dp)
                )

                Text(
                    "Enable signoff for this repository",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                PrimaryButton(
                    text = "Cancel",
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onClose,
                    backgroundColor = Color.Transparent,
                    textColor = MaterialTheme.colors.onBackground,
                )
                PrimaryButton(
                    modifier = Modifier
                        .focusRequester(buttonFieldFocusRequester)
                        .focusProperties {
                            this.previous = signOffFieldFocusRequester
                            this.next = signOffFieldFocusRequester
                        },
                    enabled = signOffField.text.isNotBlank() && state is SignOffState.Loaded,
                    onClick = {
                        viewModel.saveSignOffFormat(enabledSignOff, signOffField.text)
                        onClose()
                    },
                    text = "Save"
                )
            }
        }

        LaunchedEffect(state) {
            signOffFieldFocusRequester.requestFocus()
        }
    }
}
