package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.models.RemoteWrapper
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.viewmodels.sidepanel.RemotesViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddEditRemoteDialog(
    remotesViewModel: RemotesViewModel,
    remoteWrapper: RemoteWrapper,
    onDismiss: () -> Unit,
) {
    var remote: RemoteWrapper by remember(remoteWrapper) { mutableStateOf(remoteWrapper) }

    LaunchedEffect(Unit) {
        remotesViewModel.remoteUpdated.collectLatest { onDismiss() }
    }

    MaterialDialog(
        paddingVertical = 8.dp,
        paddingHorizontal = 16.dp,
        background = MaterialTheme.colors.surface,
        onCloseRequested = onDismiss
    ) {
        Column(
            modifier = Modifier
                .width(600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (remote.isNew) "New remote" else "Edit remote \"${remote.remoteName}\"",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.h3,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column {
                        if (remote.isNew) {
                            Text(
                                text = "Remote name",
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.padding(top = 8.dp),
                            )

                            AdjustableOutlinedTextField(
                                value = remote.remoteName,
                                onValueChange = { newValue ->
                                    remote = remote.copy(remoteName = newValue)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                        Text(
                            text = "Fetch URL",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(top = 8.dp),
                        )

                        AdjustableOutlinedTextField(
                            value = remote.fetchUri,
                            onValueChange = { newValue ->
                                remote = remote.copy(fetchUri = newValue)
                            },
                            singleLine = true,
                            colors = outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        Text(
                            text = "Push URL",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(top = 8.dp),
                        )

                        AdjustableOutlinedTextField(
                            value = remote.pushUri,
                            onValueChange = { newValue ->
                                remote = remote.copy(pushUri = newValue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .padding(top = 32.dp)
                                .align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.weight(1f))

                            PrimaryButton(
                                text = "Cancel",
                                modifier = Modifier.padding(end = 8.dp),
                                onClick = onDismiss,
                                backgroundColor = Color.Transparent,
                                backgroundDisabled = Color.Transparent,
                                textColor = MaterialTheme.colors.onBackground,
                                disabledTextColor = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                            )


                            PrimaryButton(
                                modifier = Modifier,
                                onClick = {
                                    if (remote.isNew)
                                        remotesViewModel.addRemote(remote)
                                    else
                                        remotesViewModel.updateRemote(remote)
                                },
                                text = "Save changes",
                            )
                        }
                    }
                }
            }
        }
    }
}
