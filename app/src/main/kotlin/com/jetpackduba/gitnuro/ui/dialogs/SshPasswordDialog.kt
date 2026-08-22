package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.lock
import com.jetpackduba.gitnuro.domain.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.ui.dialogs.base.PasswordDialog

@Composable
fun SshPasswordDialog(
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
    credentialsRequest: CredentialsRequest.SshCredentialsRequest,
) {
    PasswordDialog(
        title = "Introduce your SSH key's password",
        subtitle = "Your SSH key is protected with a password",
        isRetry = credentialsRequest.isRetry,
        password = credentialsRequest.password,
        retryMessage = "Invalid password, please try again",
        icon = Res.drawable.lock,
        onDismiss = onReject,
        onAccept = onAccept,
    )
}