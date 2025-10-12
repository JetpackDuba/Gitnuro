package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.lock
import com.jetpackduba.gitnuro.ui.dialogs.base.UserPasswordDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun HttpCredentialsDialog(
    onDismiss: () -> Unit,
    onAccept: (user: String, password: String) -> Unit,
) {
    UserPasswordDialog(
        title = "Introduce your remote server credentials",
        subtitle = "Your remote requires authentication with a\nusername and a password",
        icon = painterResource(Res.drawable.lock),
        onDismiss = onDismiss,
        onAccept = onAccept,
    )
}