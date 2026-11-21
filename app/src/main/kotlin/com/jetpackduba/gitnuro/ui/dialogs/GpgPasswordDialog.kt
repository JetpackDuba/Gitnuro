package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.domain.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.gpg_password_dialog_retry_message
import com.jetpackduba.gitnuro.app.generated.resources.gpg_password_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.gpg_password_dialog_title
import com.jetpackduba.gitnuro.app.generated.resources.key
import com.jetpackduba.gitnuro.ui.dialogs.base.PasswordDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun GpgPasswordDialog(
    gpgCredentialsRequest: CredentialsRequest.GpgCredentialsRequest,
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
) {
    PasswordDialog(
        title = stringResource(Res.string.gpg_password_dialog_title),
        subtitle = stringResource(Res.string.gpg_password_dialog_subtitle),
        icon = Res.drawable.key,
        cancelButtonText = "Do not sign",
        isRetry = gpgCredentialsRequest.isRetry,
        password = gpgCredentialsRequest.password,
        retryMessage = stringResource(Res.string.gpg_password_dialog_retry_message),
        onDismiss = onReject,
        onAccept = onAccept,
    )
}