package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable

@Composable
fun SshPasswordDialog(
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit
) {
    PasswordDialog(
        title = "Introduce your SSH key's password",
        subtitle = "Your SSH key is protected with a password",
        icon = "lock.svg",
        onReject = onReject,
        onAccept = onAccept,
    )
}