package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.credentials.CredentialsRequested

@Composable
fun GpgPasswordDialog(
    gpgCredentialsRequested: CredentialsRequested.GpgCredentialsRequested,
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit
) {
    PasswordDialog(
        title = "Introduce your GPG key's password",
        subtitle = "Your GPG key is protected with a password",
        icon = "key.svg",
        cancelButtonText = "Do not sign",
        isRetry = gpgCredentialsRequested.isRetry,
        password = gpgCredentialsRequested.password,
        retryMessage = "Invalid password, please try again",
        onReject = onReject,
        onAccept = onAccept,
    )
}