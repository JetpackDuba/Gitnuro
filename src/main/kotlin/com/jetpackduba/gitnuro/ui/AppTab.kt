package com.jetpackduba.gitnuro.ui

import androidx.compose.animation.Crossfade
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.LoadingRepository
import com.jetpackduba.gitnuro.ProcessingScreen
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsRequested
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.git.ProcessingState
import com.jetpackduba.gitnuro.models.Notification
import com.jetpackduba.gitnuro.models.NotificationType
import com.jetpackduba.gitnuro.theme.AppTheme
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
    val notifications = errorManager.notification.collectAsState().value
        .toList()
        .sortedBy { it.first }
        .map { it.second }

    val repositorySelectionStatus = tabViewModel.repositorySelectionStatus.collectAsState()
    val repositorySelectionStatusValue = repositorySelectionStatus.value
    val processingState = tabViewModel.processing.collectAsState().value

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (notification in notifications) {
                Notification(notification)
            }
        }
    }
}

@Preview
@Composable
fun NotificationSuccessPreview() {
    AppTheme(customTheme = null) {
        Notification(NotificationType.Positive, "Hello world!")
    }
}

@Composable
fun Notification(notification: Notification) {
    val notificationShape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .padding(8.dp)
            .border(2.dp, MaterialTheme.colors.onBackground.copy(0.2f), notificationShape)
            .clip(notificationShape)
            .background(MaterialTheme.colors.background)
            .height(IntrinsicSize.Max)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val backgroundColor = when (notification.type) {
            NotificationType.Positive -> MaterialTheme.colors.primary
            NotificationType.Warning -> MaterialTheme.colors.secondary
            NotificationType.Error -> MaterialTheme.colors.error
        }

        val contentColor = when (notification.type) {
            NotificationType.Positive -> MaterialTheme.colors.onPrimary
            NotificationType.Warning -> MaterialTheme.colors.onSecondary
            NotificationType.Error -> MaterialTheme.colors.onError
        }

        val icon = when (notification.type) {
            NotificationType.Positive -> AppIcons.INFO
            NotificationType.Warning -> AppIcons.WARNING
            NotificationType.Error -> AppIcons.ERROR
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .background(backgroundColor)
                .fillMaxHeight()
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .padding(4.dp)
            )
        }

        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = notification.text,
                modifier = Modifier,
                color = MaterialTheme.colors.onBackground,
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
