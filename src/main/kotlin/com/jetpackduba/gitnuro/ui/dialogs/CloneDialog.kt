package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.git.CloneStatus
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors

import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.openDirectoryDialog
import com.jetpackduba.gitnuro.viewmodels.CloneViewModel
import java.io.File

@Composable
fun CloneDialog(
    cloneViewModel: CloneViewModel,
    onClose: () -> Unit,
    onOpenRepository: (File) -> Unit,
) {
    val cloneStatus = cloneViewModel.cloneStatus.collectAsState()
    val cloneStatusValue = cloneStatus.value

    MaterialDialog(
        onCloseRequested = onClose,
        background = MaterialTheme.colors.surface,
    ) {
        Box(
            modifier = Modifier
                .width(720.dp)
                .height(280.dp)
                .animateContentSize()
        ) {
            when (cloneStatusValue) {
                is CloneStatus.Cloning -> {
                    Cloning(cloneViewModel, cloneStatusValue)
                }

                is CloneStatus.Cancelling -> {
                    Cancelling()
                }

                is CloneStatus.Completed -> {
                    onOpenRepository(cloneStatusValue.repoDir)
                    onClose()
                }

                is CloneStatus.Fail -> CloneInput(
                    cloneViewModel = cloneViewModel,
                    onClose = onClose,
                    errorMessage = cloneStatusValue.reason
                )

                CloneStatus.None -> CloneInput(
                    cloneViewModel = cloneViewModel,
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
private fun CloneInput(
    cloneViewModel: CloneViewModel,
    onClose: () -> Unit,
    errorMessage: String? = null,
) {
    var url by remember { mutableStateOf(cloneViewModel.url) }
    var directory by remember { mutableStateOf(cloneViewModel.directory) }

    val urlFocusRequester = remember { FocusRequester() }
    val directoryFocusRequester = remember { FocusRequester() }
    val directoryButtonFocusRequester = remember { FocusRequester() }
    val cloneButtonFocusRequester = remember { FocusRequester() }
    val cancelButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        urlFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            "Clone a new repository",
            style = MaterialTheme.typography.h3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
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
            onValueChange = {
                cloneViewModel.resetStateIfError()
                url = it
                cloneViewModel.url = url
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            TextInput(
                modifier = Modifier.weight(1f),
                title = "Directory",
                value = directory,
                focusRequester = directoryFocusRequester,
                focusProperties = {
                    previous = urlFocusRequester
                    next = directoryButtonFocusRequester
                },
                onValueChange = {
                    cloneViewModel.resetStateIfError()
                    directory = it
                    cloneViewModel.directory = directory
                },
                textFieldShape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
            )

            Button(
                onClick = {
                    cloneViewModel.resetStateIfError()
                    val newDirectory = openDirectoryDialog()
                    if (newDirectory != null) {
                        directory = newDirectory
                        cloneViewModel.directory = directory
                    }
                },
                modifier = Modifier
                    .focusRequester(directoryButtonFocusRequester)
                    .focusProperties {
                        previous = directoryFocusRequester
                        next = cloneButtonFocusRequester
                    }
                    .height(48.dp),
                shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                )
            }
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colors.error)
            ) {
                Text(
                    errorMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    color = MaterialTheme.colors.onError,
                )
            }

        }

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.End)
        ) {
            TextButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .focusRequester(cancelButtonFocusRequester)
                    .focusProperties {
                        previous = cloneButtonFocusRequester
                        next = urlFocusRequester
                    },
                colors = textButtonColors(),
                onClick = {
                    onClose()
                }
            ) {
                Text("Cancel")
            }
            PrimaryButton(
                onClick = {
                    cloneViewModel.clone(directory, url)
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
    }
}

@Composable
private fun Cloning(cloneViewModel: CloneViewModel, cloneStatusValue: CloneStatus.Cloning) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        val progress = remember(cloneStatusValue) {
            val total = cloneStatusValue.total

            if (total == 0) // Prevent division by 0
                -1f
            else
                cloneStatusValue.progress / total.toFloat()
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(cloneStatusValue.taskName, color = MaterialTheme.colors.onBackground)

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
            Text("Cancel")
        }
    }
}

@Composable
private fun Cancelling() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
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
    value: String,
    enabled: Boolean = true,
    focusRequester: FocusRequester,
    focusProperties: FocusProperties.() -> Unit,
    onValueChange: (String) -> Unit,
    textFieldShape: Shape = RoundedCornerShape(4.dp),
) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .width(100.dp)
                .padding(end = 16.dp),
        )

        AdjustableOutlinedTextField(
            value = value,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .focusProperties(focusProperties),
            enabled = enabled,
            onValueChange = onValueChange,
            colors = outlinedTextFieldColors(),
            singleLine = true,
            shape = textFieldShape,
        )
    }
}