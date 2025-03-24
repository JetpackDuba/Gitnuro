package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.lock

@Composable
fun SshPasswordDialog(
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
) {
    PasswordDialog(
        title = "Introduce your SSH key's password",
        subtitle = "Your SSH key is protected with a password",
        icon = Res.drawable.lock,
        onReject = onReject,
        onAccept = onAccept,
    )
}