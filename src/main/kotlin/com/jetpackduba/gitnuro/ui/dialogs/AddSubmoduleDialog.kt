package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.topic
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.dialogs.base.IconBasedDialog
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SubmoduleDialogViewModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun AddSubmodulesDialog(
    viewModel: SubmoduleDialogViewModel,
    onDismiss: () -> Unit,
    onAccept: (repository: String, directory: String) -> Unit,
) {
    var repositoryField by remember { mutableStateOf("") }
    var directoryField by remember { mutableStateOf(TextFieldValue("")) }
    val repositoryFocusRequester = remember { FocusRequester() }
    val directoryFocusRequester = remember { FocusRequester() }
    val actionsFocusRequester = remember { FocusRequester() }

    val error by viewModel.error.collectAsState()
    val isValidValue = repositoryField.isNotBlank() && directoryField.text.isNotBlank()

    fun doAccept() {
        viewModel.verifyData(repositoryField, directoryField.text)
    }

    LaunchedEffect(viewModel) {
        viewModel.onDataIsValid.collect {
            onAccept(repositoryField, directoryField.text)
        }
    }



    IconBasedDialog(
        icon = painterResource(Res.drawable.topic),
        title = "New submodule",
        subtitle = "Create a new submodule from an existing repository",
        primaryActionText = "Create submodule",
        isPrimaryActionEnabled = isValidValue,
        beforeActionsFocusRequester = directoryFocusRequester,
        actionsFocusRequester = actionsFocusRequester,
        afterActionsFocusRequester = repositoryFocusRequester,
        onDismiss = onDismiss,
        onPrimaryActionClicked = ::doAccept
    ) {

        Text(
            "Repository URL",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
                .align(Alignment.Start),
        )

        AdjustableOutlinedTextField(
            modifier = Modifier
                .focusRequester(repositoryFocusRequester)
                .focusProperties {
                    this.next = directoryFocusRequester
                }
                .width(480.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && directoryField.text.isBlank() && repositoryField.isNotBlank()) {
                        val repo = repositoryField.split("/", "\\").last().removeSuffix(".git")
                        directoryField = TextFieldValue(repo, selection = TextRange(repo.count()))
                    }
                },
            value = repositoryField,
            maxLines = 1,
            onValueChange = {
                viewModel.resetError()
                repositoryField = it
            },
        )

        Text(
            "Directory",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                .align(Alignment.Start),
        )

        AdjustableOutlinedTextField(
            modifier = Modifier
                .focusRequester(directoryFocusRequester)
                .focusProperties {
                    this.next = actionsFocusRequester
                }
                .width(480.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) && isValidValue) {
                        doAccept()
                        true
                    } else {
                        false
                    }
                },
            value = directoryField,
            maxLines = 1,
            onValueChange = {
                viewModel.resetError()
                directoryField = it
            },
        )

        AnimatedVisibility(error.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colors.error)
            ) {
                Text(
                    error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    color = MaterialTheme.colors.onError,
                )
            }
        }

        LaunchedEffect(Unit) {
            repositoryFocusRequester.requestFocus()
        }
    }
}