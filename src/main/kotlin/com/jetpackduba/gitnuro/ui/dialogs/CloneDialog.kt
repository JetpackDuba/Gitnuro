package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.git.CloneState
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors

import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.viewmodels.CloneViewModel
import java.io.File

@Composable
fun CloneDialog(
    cloneViewModel: CloneViewModel = gitnuroViewModel(),
    onClose: () -> Unit,
    onOpenRepository: (File) -> Unit,
) {
    val cloneStatus = cloneViewModel.cloneState.collectAsState()
    val cloneStatusValue = cloneStatus.value

    MaterialDialog(
        onCloseRequested = onClose,
        background = MaterialTheme.colors.surface,
    ) {
        Box(
            modifier = Modifier
                .width(720.dp)
                .animateContentSize()
        ) {
            when (cloneStatusValue) {
                is CloneState.Cloning -> {
                    Cloning(cloneViewModel, cloneStatusValue)
                }

                is CloneState.Cancelling -> {
                    Cancelling()
                }

                is CloneState.Completed -> {
                    onOpenRepository(cloneStatusValue.repoDir)
                    onClose()
                }

                is CloneState.Fail, CloneState.None -> CloneDialogView(
                    cloneViewModel = cloneViewModel,
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
private fun CloneDialogView(
    cloneViewModel: CloneViewModel,
    onClose: () -> Unit,
) {
    var url by remember(cloneViewModel) { mutableStateOf(cloneViewModel.repositoryUrl.value) }
    var directory by remember(cloneViewModel) { mutableStateOf(cloneViewModel.directoryPath.value) }
    var cloneSubmodules by remember { mutableStateOf(true) }

    val error by cloneViewModel.error.collectAsState()

    val urlFocusRequester = remember { FocusRequester() }
    val directoryFocusRequester = remember { FocusRequester() }
    val directoryButtonFocusRequester = remember { FocusRequester() }
    val cloneButtonFocusRequester = remember { FocusRequester() }
    val cancelButtonFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            "Clone a new repository",
            style = MaterialTheme.typography.h4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            fontWeight = FontWeight.SemiBold,
        )

        TextInput(
            modifier = Modifier.padding(top = 8.dp),
            title = "URL",
            value = url,
            focusRequester = urlFocusRequester,
            focusProperties = {
                previous = cancelButtonFocusRequester
                next = directoryFocusRequester
            },
            onValueChange = { repositoryUrl ->
                url = repositoryUrl
                cloneViewModel.onRepositoryUrlChanged(repositoryUrl)
                cloneViewModel.resetStateIfError()
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            TextInput(
                modifier = Modifier.padding(top = 16.dp),
                title = "Directory",
                value = directory,
                focusRequester = directoryFocusRequester,
                focusProperties = {
                    previous = urlFocusRequester
                    next = directoryButtonFocusRequester
                },
                onValueChange = {
                    directory = it
                    cloneViewModel.onDirectoryPathChanged(directory)
                    cloneViewModel.resetStateIfError()
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            cloneViewModel.resetStateIfError()
                            val newDirectory = cloneViewModel.openDirectoryPicker()
                            if (newDirectory != null) {
                                directory = TextFieldValue(newDirectory, selection = TextRange(newDirectory.count()))
                                cloneViewModel.onDirectoryPathChanged(directory)
                                cloneViewModel.resetStateIfError()
                                directoryFocusRequester.requestFocus()
                            }
                        },
                        modifier = Modifier
                            .focusRequester(directoryButtonFocusRequester)
                            .focusProperties {
                                previous = directoryFocusRequester
                                next = cloneButtonFocusRequester
                            }
                            .handOnHover()
                            .size(40.dp),
                    ) {
                        Icon(
                            painterResource(AppIcons.SEARCH),
                            contentDescription = "Search",
                            tint = MaterialTheme.colors.onBackground,
                        )
                    }
                }
            )


        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.handMouseClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                cloneSubmodules = !cloneSubmodules
            }
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Checkbox(
                checked = cloneSubmodules,
                onCheckedChange = {
                    cloneSubmodules = it
                },
                modifier = Modifier
                    .padding(all = 8.dp)
                    .size(12.dp)
            )

            Text(
                "Clone submodules recursively",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )
        }

        AnimatedVisibility (error.isNotBlank()) {
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
                .padding(top = 16.dp)
                .align(Alignment.End)
        ) {
            PrimaryButton(
                text = "Cancel",
                modifier = Modifier.padding(end = 8.dp)
                    .focusRequester(cancelButtonFocusRequester)
                    .focusProperties {
                        previous = cloneButtonFocusRequester
                        next = urlFocusRequester
                    },
                onClick = onClose,
                backgroundColor = Color.Transparent,
                textColor = MaterialTheme.colors.onBackground,
            )
            PrimaryButton(
                onClick = {
                    cloneViewModel.clone(directory.text, url.text, cloneSubmodules)
                },
                modifier = Modifier
                    .focusRequester(cloneButtonFocusRequester)
                    .focusProperties {
                        previous = directoryButtonFocusRequester
                        next = cancelButtonFocusRequester
                    },
                text = "Clone"
            )
        }

        LaunchedEffect(Unit) {
            urlFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun Cloning(cloneViewModel: CloneViewModel, cloneStateValue: CloneState.Cloning) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        val progress = remember(cloneStateValue) {
            val total = cloneStateValue.total

            if (total == 0) // Prevent division by 0
                -1f
            else
                cloneStateValue.progress / total.toFloat()
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(cloneStateValue.taskName, color = MaterialTheme.colors.onBackground)

            if (progress >= 0f)
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    progress = progress,
                    color = MaterialTheme.colors.primaryVariant,
                )
            else // Show indeterminate if we do not know the total (aka total == 0 or progress == -1)
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    color = MaterialTheme.colors.primaryVariant,
                )

        }

        TextButton(
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.BottomEnd),
            colors = textButtonColors(),
            onClick = {
                cloneViewModel.cancelClone()
            }
        ) {
            Text(
                text = "Cancel",
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
            )
        }
    }
}

@Composable
private fun Cancelling() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colors.primaryVariant,
        )

        Text(
            text = "Cancelling clone operation...",
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun TextInput(
    modifier: Modifier = Modifier,
    title: String,
    value: TextFieldValue,
    enabled: Boolean = true,
    focusRequester: FocusRequester,
    focusProperties: FocusProperties.() -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    textFieldShape: Shape = RoundedCornerShape(4.dp),
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(bottom = 8.dp),
        )

        AdjustableOutlinedTextField(
            value = value,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusProperties(focusProperties),
            enabled = enabled,
            onValueChange = onValueChange,
            colors = outlinedTextFieldColors(),
            singleLine = true,
            shape = textFieldShape,
            trailingIcon = trailingIcon,
        )
    }
}