package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.sign
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.dialogs.base.IconBasedDialog
import com.jetpackduba.gitnuro.viewmodels.SignOffDialogViewModel
import com.jetpackduba.gitnuro.viewmodels.SignOffState
import org.jetbrains.compose.resources.painterResource

@Composable
fun SignOffDialog(
    viewModel: SignOffDialogViewModel,
    onDismiss: () -> Unit,
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

    IconBasedDialog(
        icon = painterResource(Res.drawable.sign),
        title = "Edit sign-off",
        subtitle = "Enable or disable the sign-off or adjust its format",
        primaryActionText = "Save",
        showCancelAction = false,
        onDismiss = onDismiss,
        onPrimaryActionClicked = onDismiss,
        beforeActionsFocusRequester = null,
        actionsFocusRequester = null,
        afterActionsFocusRequester = null,
    ) {

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
                "Enable sign-off for this repository",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )
        }
    }
}
