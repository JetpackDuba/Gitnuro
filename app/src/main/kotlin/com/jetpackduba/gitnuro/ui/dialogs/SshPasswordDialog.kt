package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.lock
import com.jetpackduba.gitnuro.app.generated.resources.ssh_password_dialog_retry
import com.jetpackduba.gitnuro.app.generated.resources.ssh_password_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.ssh_password_dialog_title
import com.jetpackduba.gitnuro.domain.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.ui.dialogs.base.PasswordDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun SshPasswordDialog(
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
    credentialsRequest: CredentialsRequest.SshCredentialsRequest,
) {
    PasswordDialog(
        title = stringResource(Res.string.ssh_password_dialog_title),
        subtitle = stringResource(Res.string.ssh_password_dialog_subtitle),
        isRetry = credentialsRequest.isRetry,
        password = credentialsRequest.password,
        retryMessage = stringResource(Res.string.ssh_password_dialog_retry),
        icon = Res.drawable.lock,
        onDismiss = onReject,
        onAccept = onAccept,
    )
}