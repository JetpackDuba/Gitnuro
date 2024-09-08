package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsRequested
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.viewmodels.TabViewModel

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