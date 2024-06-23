package com.jetpackduba.gitnuro.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.LoadingRepository
import com.jetpackduba.gitnuro.ProcessingScreen
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsRequested
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.git.ProcessingState
import com.jetpackduba.gitnuro.ui.dialogs.CloneDialog
import com.jetpackduba.gitnuro.ui.dialogs.GpgPasswordDialog
import com.jetpackduba.gitnuro.ui.dialogs.SshPasswordDialog
import com.jetpackduba.gitnuro.ui.dialogs.UserPasswordDialog
import com.jetpackduba.gitnuro.ui.dialogs.errors.ErrorDialog
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
    val notification = errorManager.notification.collectAsState().value
    var visibleNotification by remember { mutableStateOf("") }
//    val (tabPosition, setTabPosition) = remember { mutableStateOf<LayoutCoordinates?>(null) }

    val repositorySelectionStatus = tabViewModel.repositorySelectionStatus.collectAsState()
    val repositorySelectionStatusValue = repositorySelectionStatus.value
    val processingState = tabViewModel.processing.collectAsState().value

    LaunchedEffect(notification) {
        if (notification != null) {
            visibleNotification = notification
        }
    }

    LaunchedEffect(tabViewModel) {
        // Init the tab content when the tab is selected and also remove the "initialPath" to avoid opening the
        // repository everytime the user changes between tabs
        val initialPath = tabViewModel.initialPath
        tabViewModel.initialPath = null

        if (initialPath != null) {
            tabViewModel.openRepository(initialPath)
        }
    }

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
                    settingsViewModel = tabViewModel.tabViewModelsProvider.settingsViewModel,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            var showCloneDialog by remember { mutableStateOf(false) }

            if (showCloneDialog) {
                CloneDialog(
                    cloneViewModel = tabViewModel.tabViewModelsProvider.cloneViewModel,
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

        if (processingState is ProcessingState.Processing) {
            ProcessingScreen(
                processingState,
                onCancelOnGoingTask = { tabViewModel.cancelOngoingTask() }
            )
        }


        val safeLastError = lastError
        if (safeLastError != null && showError) {
            ErrorDialog(
                error = safeLastError,
                onAccept = { tabViewModel.showError.value = false }
            )
        }

        AnimatedVisibility(
            visible = notification != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it * 2 },
            exit = fadeOut() + slideOutVertically { it * 2 },
        ) {
            Text(
                text = visibleNotification,
                modifier = Modifier
                    .padding(bottom = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.primary)
                    .padding(8.dp),
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.body1,
            )
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
