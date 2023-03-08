package com.jetpackduba.gitnuro.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.LoadingRepository
import com.jetpackduba.gitnuro.LocalTabScope
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsRequested
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.dialogs.*
import com.jetpackduba.gitnuro.ui.dialogs.settings.SettingsDialog
import com.jetpackduba.gitnuro.viewmodels.RepositorySelectionStatus
import com.jetpackduba.gitnuro.viewmodels.TabViewModel

@Composable
fun AppTab(
    tabViewModel: TabViewModel,
) {
    val errorManager = tabViewModel.errorsManager
    val lastError by errorManager.error.collectAsState(null)
    val showError by tabViewModel.showError.collectAsState()

    val repositorySelectionStatus = tabViewModel.repositorySelectionStatus.collectAsState()
    val repositorySelectionStatusValue = repositorySelectionStatus.value
    val isProcessing by tabViewModel.processing.collectAsState()

    Box {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {

            CredentialsDialog(tabViewModel)

            var showSettingsDialog by remember { mutableStateOf(false) }
            if (showSettingsDialog) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog = false }
                )
            }

            var showCloneDialog by remember { mutableStateOf(false) }

            if (showCloneDialog) {
                CloneDialog(
                    onClose = {
                        showCloneDialog = false
                    },
                    onOpenRepository = { dir ->
                        tabViewModel.openRepository(dir)
                    },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = repositorySelectionStatus) {
                    when (repositorySelectionStatusValue) {
                        RepositorySelectionStatus.None -> {
                            WelcomePage(
                                tabViewModel = tabViewModel,
                                onShowCloneDialog = { showCloneDialog = true },
                                onShowSettings = { showSettingsDialog = true }
                            )
                        }

                        is RepositorySelectionStatus.Opening -> {
                            LoadingRepository(repositorySelectionStatusValue.path)
                        }

                        is RepositorySelectionStatus.Open -> {
                            RepositoryOpenPage(
                                tabViewModel = tabViewModel,
                                onShowSettingsDialog = { showSettingsDialog = true },
                                onShowCloneDialog = { showCloneDialog = true },
                            )
                        }
                    }
                }
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
                    .onPreviewKeyEvent { true }, // Disable all keyboard events
                contentAlignment = Alignment.Center,
            ) {
                Column {
                    LinearProgressIndicator(
                        modifier = Modifier.width(340.dp),
                        color = MaterialTheme.colors.secondary,
                    )
                }
            }
        }


        val safeLastError = lastError
        if (safeLastError != null && showError) {
            MaterialDialog {
                Column(
                    modifier = Modifier
                        .width(580.dp)
                ) {
                    Row {
                        Text(
                            text = "Error",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colors.onBackground,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            painterResource(AppIcons.ERROR),
                            contentDescription = null,
                            tint = MaterialTheme.colors.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = lastError?.message ?: "",
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .widthIn(max = 600.dp),
                        style = MaterialTheme.typography.body2,
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 32.dp)
                    ) {
                        PrimaryButton(text = "OK", onClick = { tabViewModel.showError.value = false })
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialsDialog(gitManager: TabViewModel) {
    val credentialsState = gitManager.credentialsState.collectAsState()

    when (val credentialsStateValue = credentialsState.value) {
        CredentialsRequested.HttpCredentialsRequested -> {
            UserPasswordDialog(
                onReject = {
                    gitManager.credentialsDenied()
                },
                onAccept = { user, password ->
                    gitManager.httpCredentialsAccepted(user, password)
                }
            )
        }

        CredentialsRequested.SshCredentialsRequested -> {
            SshPasswordDialog(
                onReject = {
                    gitManager.credentialsDenied()
                },
                onAccept = { password ->
                    gitManager.sshCredentialsAccepted(password)
                }
            )
        }

        is CredentialsRequested.GpgCredentialsRequested -> {
            GpgPasswordDialog(
                gpgCredentialsRequested = credentialsStateValue,
                onReject = {
                    gitManager.credentialsDenied()
                },
                onAccept = { password ->
                    gitManager.gpgCredentialsAccepted(password)
                }
            )
        }

        is CredentialsAccepted, CredentialsState.None, CredentialsState.CredentialsDenied -> { /* Nothing to do */
        }
    }
}
