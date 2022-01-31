package app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.LoadingRepository
import app.credentials.CredentialsState
import app.viewmodels.TabViewModel
import app.viewmodels.RepositorySelectionStatus
import app.ui.dialogs.PasswordDialog
import app.ui.dialogs.UserPasswordDialog
import kotlinx.coroutines.delay

// TODO onDispose sometimes is called when changing tabs, therefore losing the tab state
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppTab(
    tabViewModel: TabViewModel,
) {
    val errorManager = tabViewModel.errorsManager
    val lastError by errorManager.lastError.collectAsState()
    var showError by remember { mutableStateOf(false) }

    if (lastError != null) {
        LaunchedEffect(lastError) {
            showError = true
            delay(5000)
            showError = false
        }
    }

    val repositorySelectionStatus by tabViewModel.repositorySelectionStatus.collectAsState()
    val isProcessing by tabViewModel.processing.collectAsState()

    Box {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {
            val linearProgressAlpha = if (isProcessing)
                DefaultAlpha
            else
                0f

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(linearProgressAlpha)
            )

            CredentialsDialog(tabViewModel)

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = repositorySelectionStatus) {
                    @Suppress("UnnecessaryVariable") // Don't inline it because smart cast won't work
                    when (repositorySelectionStatus) {
                        RepositorySelectionStatus.None -> {
                            WelcomePage(tabViewModel = tabViewModel)
                        }
                        RepositorySelectionStatus.Loading -> {
                            LoadingRepository()
                        }
                        is RepositorySelectionStatus.Open -> {
                            RepositoryOpenPage(tabViewModel = tabViewModel)
                        }
                    }
                }

                if (isProcessing)
                    Box(modifier = Modifier.fillMaxSize()) //TODO this should block of the mouse/keyboard events while visible
            }
        }

        val safeLastError = lastError
        if (safeLastError != null) {
            AnimatedVisibility(
                visible = showError,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 32.dp)
            ) {
                val interactionSource = remember { MutableInteractionSource() }

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
                            color = Color.White,
                        ) // TODO Add more  descriptive title

                        Text(
                            text = lastError?.message ?: "",
                            color = Color.White,
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
