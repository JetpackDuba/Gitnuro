package com.jetpackduba.gitnuro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.LoadingRepository
import com.jetpackduba.gitnuro.LocalTabScope
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.ui.dialogs.PasswordDialog
import com.jetpackduba.gitnuro.ui.dialogs.UserPasswordDialog
import com.jetpackduba.gitnuro.viewmodels.RepositorySelectionStatus
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import kotlinx.coroutines.delay

@Composable
fun AppTab(
    tabViewModel: TabViewModel,
) {
    val errorManager = tabViewModel.errorsManager
    val lastError by errorManager.error.collectAsState(null)
    val showError by tabViewModel.showError.collectAsState()

    if (lastError != null) {
        LaunchedEffect(lastError) {
            tabViewModel.showError.value = true
            delay(5000)
            tabViewModel.showError.value = false
        }
    }

    val repositorySelectionStatus = tabViewModel.repositorySelectionStatus.collectAsState()
    val repositorySelectionStatusValue = repositorySelectionStatus.value
    val isProcessing by tabViewModel.processing.collectAsState()

    LocalTabScope.current.appStateManager
    Box {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {

            CredentialsDialog(tabViewModel)

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = repositorySelectionStatus) {
                    when (repositorySelectionStatusValue) {
                        RepositorySelectionStatus.None -> {
                            WelcomePage(tabViewModel = tabViewModel)
                        }

                        is RepositorySelectionStatus.Opening -> {
                            LoadingRepository(repositorySelectionStatusValue.path)
                        }

                        is RepositorySelectionStatus.Open -> {
                            RepositoryOpenPage(tabViewModel = tabViewModel)
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
        if (safeLastError != null) {
            AnimatedVisibility(
                visible = showError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 32.dp)
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                // TODO: Rework popup to appear on top of every other UI component, even dialogs
                Card(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 200.dp, minHeight = 100.dp)
                        .clickable(
                            enabled = true,
                            onClick = {},
                            interactionSource = interactionSource,
                            indication = null
                        ),
                    backgroundColor = MaterialTheme.colors.error,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 16.dp),
                            color = MaterialTheme.colors.onError,
                        ) // TODO Add more  descriptive title

                        Text(
                            text = lastError?.message ?: "",
                            color = MaterialTheme.colors.onError,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 16.dp)
                                .widthIn(max = 600.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialsDialog(gitManager: TabViewModel) {
    val credentialsState by gitManager.credentialsState.collectAsState()

    if (credentialsState == CredentialsState.HttpCredentialsRequested) {
        UserPasswordDialog(
            onReject = {
                gitManager.credentialsDenied()
            },
            onAccept = { user, password ->
                gitManager.httpCredentialsAccepted(user, password)
            }
        )
    } else if (credentialsState == CredentialsState.SshCredentialsRequested) {
        PasswordDialog(
            onReject = {
                gitManager.credentialsDenied()
            },
            onAccept = { password ->
                gitManager.sshCredentialsAccepted(password)
            }
        )
    }
}
