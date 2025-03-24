package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.lfs
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun CredentialsDialog(tabViewModel: TabViewModel) {
    val credentialsState = tabViewModel.credentialsState.collectAsState()

    when (val credentialsStateValue = credentialsState.value) {
        CredentialsRequest.HttpCredentialsRequest -> {
            UserPasswordDialog(
                onReject = {
                    tabViewModel.credentialsDenied()
                },
                onAccept = { user, password ->
                    tabViewModel.httpCredentialsAccepted(user, password)
                }
            )
        }

        CredentialsRequest.SshCredentialsRequest -> {
            SshPasswordDialog(
                onReject = {
                    tabViewModel.credentialsDenied()
                },
                onAccept = { password ->
                    tabViewModel.sshCredentialsAccepted(password)
                }
            )
        }

        is CredentialsRequest.GpgCredentialsRequest -> {
            GpgPasswordDialog(
                gpgCredentialsRequest = credentialsStateValue,
                onReject = {
                    tabViewModel.credentialsDenied()
                },
                onAccept = { password ->
                    tabViewModel.gpgCredentialsAccepted(password)
                }
            )
        }

        CredentialsRequest.LfsCredentialsRequest -> {
            UserPasswordDialog(
                title = "LFS Server Credentials",
                subtitle = "Introduce the credentials for your LFS server",
                icon = painterResource(Res.drawable.lfs),
                onReject = {
                    tabViewModel.credentialsDenied()
                },
                onAccept = { user, password ->
                    tabViewModel.lfsCredentialsAccepted(user, password)
                }
            )
        }

        is CredentialsAccepted, CredentialsState.None, CredentialsState.CredentialsDenied -> { /* Nothing to do */
        }

    }
}