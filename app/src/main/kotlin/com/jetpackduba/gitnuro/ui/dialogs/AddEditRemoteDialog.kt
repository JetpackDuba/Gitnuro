package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog

@Composable
fun AddEditRemoteDialog(
    viewModel: AddEditRemoteViewModel,
    onDismiss: () -> Unit,
) {
    val remote by viewModel.remote.collectAsState()
    val isNew = viewModel.isNewRemote

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
                    text = if (isNew) "New remote" else "Edit remote \"${remote.name}\"",
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
                        if (isNew) {
                            Text(
                                text = "Remote name",
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.padding(top = 8.dp),
                            )

                            AdjustableOutlinedTextField(
                                value = remote.name,
                                onValueChange = { newValue ->
                                    viewModel.updateRemoteName(newValue)
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
                                if (remote.pushUri == remote.fetchUri) {
                                    viewModel.updateAllUri(newValue)
                                } else {
                                    viewModel.updateFetchUri(newValue)
                                }
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
                                viewModel.updatePushUri(newValue)
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
                                    viewModel.save()
                                    onDismiss() // TODO This should be only dimissed if save worked
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
