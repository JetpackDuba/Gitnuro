package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.key

@Composable
fun GpgPasswordDialog(
    gpgCredentialsRequest: CredentialsRequest.GpgCredentialsRequest,
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
) {
    PasswordDialog(
        title = "Introduce your GPG key's password",
        subtitle = "Your GPG key is protected with a password",
        icon = Res.drawable.key,
        cancelButtonText = "Do not sign",
        isRetry = gpgCredentialsRequest.isRetry,
        password = gpgCredentialsRequest.password,
        retryMessage = "Invalid password, please try again",
        onReject = onReject,
        onAccept = onAccept,
    )
}