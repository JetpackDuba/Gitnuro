package app.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.git.CloneStatus
import app.theme.outlinedTextFieldColors
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.ui.components.PrimaryButton
import app.ui.openDirectoryDialog
import app.viewmodels.CloneViewModel
import java.io.File

@Composable
fun CloneDialog(
    cloneViewModel: CloneViewModel,
    onClose: () -> Unit,
    onOpenRepository: (File) -> Unit,
) {
    val cloneStatus = cloneViewModel.cloneStatus.collectAsState()
    val cloneStatusValue = cloneStatus.value

    MaterialDialog(onCloseRequested = onClose) {
        Box(
            modifier = Modifier
                .width(400.dp)
                .animateContentSize()
        ) {
            when (cloneStatusValue) {
                is CloneStatus.Cloning -> {
                    Cloning(cloneViewModel, cloneStatusValue)
                }

                is CloneStatus.Cancelling -> {
                    onClose()
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
    ) {
        Text(
            "Clone a new repository",
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .focusRequester(urlFocusRequester)
                .focusProperties {
                    previous = cancelButtonFocusRequester
                    next = directoryFocusRequester
                },
            label = { Text("URL") },
            textStyle = MaterialTheme.typography.body1,
            maxLines = 1,
            value = url,
            colors = outlinedTextFieldColors(),
            onValueChange = {
                cloneViewModel.resetStateIfError()
                url = it
                cloneViewModel.url = url
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .focusRequester(directoryFocusRequester)
                    .focusProperties {
                        previous = urlFocusRequester
                        next = directoryButtonFocusRequester
                    },
                textStyle = MaterialTheme.typography.body1,
                maxLines = 1,
                label = { Text("Directory") },
                value = directory,
                colors = outlinedTextFieldColors(),
                onValueChange = {
                    cloneViewModel.resetStateIfError()
                    directory = it
                    cloneViewModel.directory = directory
                }
            )

            IconButton(
                onClick = {
                    cloneViewModel.resetStateIfError()
                    val newDirectory = openDirectoryDialog()
                    if (newDirectory != null)
                        directory = newDirectory
                },
                modifier = Modifier
                    .focusRequester(directoryButtonFocusRequester)
                    .focusProperties {
                        previous = directoryFocusRequester
                        next = cloneButtonFocusRequester
                    }
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primaryTextColor,
                )
            }
        }

        AnimatedVisibility(errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colors.error)
            ) {
                Text(
                    errorMessage.orEmpty(),
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
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {


        val progress = remember(cloneStatusValue) {
            val total = cloneStatusValue.total

            if (total == 0) // Prevent division by 0
                -1f
            else
                cloneStatusValue.progress / total.toFloat()
        }

        if (progress >= 0f)
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 16.dp),
                progress = progress
            )
        else // Show indeterminate if we do not know the total (aka total == 0 or progress == -1)
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 16.dp),
            )

        Text(cloneStatusValue.taskName, color = MaterialTheme.colors.primaryTextColor)

        TextButton(
            modifier = Modifier
                .padding(
                    top = 36.dp,
                    end = 8.dp
                )
                .align(Alignment.End),
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
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text(
            text = "Cancelling clone operation...",
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}