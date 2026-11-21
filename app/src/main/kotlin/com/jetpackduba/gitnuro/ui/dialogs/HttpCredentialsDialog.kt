package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.http_credentials_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.http_credentials_dialog_title
import com.jetpackduba.gitnuro.app.generated.resources.lock
import com.jetpackduba.gitnuro.ui.dialogs.base.UserPasswordDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HttpCredentialsDialog(
    onDismiss: () -> Unit,
    onAccept: (user: String, password: String) -> Unit,
) {
    UserPasswordDialog(
        title = stringResource(Res.string.http_credentials_dialog_title),
        subtitle = stringResource(Res.string.http_credentials_dialog_subtitle),
        icon = painterResource(Res.drawable.lock),
        onDismiss = onDismiss,
        onAccept = onAccept,
    )
}