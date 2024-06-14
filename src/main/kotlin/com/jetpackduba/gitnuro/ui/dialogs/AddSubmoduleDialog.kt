package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.viewmodels.sidepanel.SubmoduleDialogViewModel

@Composable
fun AddSubmodulesDialog(
    viewModel: SubmoduleDialogViewModel,
    onCancel: () -> Unit,
    onAccept: (repository: String, directory: String) -> Unit,
) {
    var repositoryField by remember { mutableStateOf("") }
    var directoryField by remember { mutableStateOf(TextFieldValue("")) }
    val repositoryFocusRequester = remember { FocusRequester() }
    val directoryFocusRequester = remember { FocusRequester() }
    val buttonFieldFocusRequester = remember { FocusRequester() }

    val error by viewModel.error.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onDataIsValid.collect {
            onAccept(repositoryField, directoryField.text)
        }
    }

    MaterialDialog(
        background = MaterialTheme.colors.surface,
        onCloseRequested = onCancel
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.animateContentSize().width(IntrinsicSize.Min)
        ) {
            Icon(
                painterResource(AppIcons.TOPIC),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp),
                tint = MaterialTheme.colors.onBackground,
            )

            Text(
                text = "New submodule",
                modifier = Modifier
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "Create a new submodule from an existing repository",
                modifier = Modifier
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

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
                        this.next = buttonFieldFocusRequester
                    }
                    .width(480.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) && repositoryField.isNotBlank() && directoryField.text.isNotBlank()) {
                            viewModel.verifyData(repositoryField, directoryField.text)
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

            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .align(Alignment.End)
            ) {
                PrimaryButton(
                    text = "Cancel",
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onCancel,
                    backgroundColor = Color.Transparent,
                    textColor = MaterialTheme.colors.onBackground,
                )
                PrimaryButton(
                    modifier = Modifier
                        .focusRequester(buttonFieldFocusRequester)
                        .focusProperties {
                            this.previous = repositoryFocusRequester
                            this.next = repositoryFocusRequester
                        },
                    enabled = repositoryField.isNotBlank() && directoryField.text.isNotBlank(),
                    onClick = {
                        viewModel.verifyData(repositoryField, directoryField.text)
                    },
                    text = "Create submodule"
                )
            }
        }

        LaunchedEffect(Unit) {
            repositoryFocusRequester.requestFocus()
        }
    }
}