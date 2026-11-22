package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.stash
import com.jetpackduba.gitnuro.app.generated.resources.stash_with_message_dialog_action
import com.jetpackduba.gitnuro.app.generated.resources.stash_with_message_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.stash_with_message_dialog_title
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun StashWithMessageDialog(
    viewModel: StashWithMessageViewModel,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf("") }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.stash),
        title = stringResource(Res.string.stash_with_message_dialog_title),
        subtitle = stringResource(Res.string.stash_with_message_dialog_subtitle),
        value = field,
        onValueChange = { field = it },
        isPrimaryActionEnabled = field.isNotBlank(),
        primaryActionText = stringResource(Res.string.stash_with_message_dialog_action),
        onDismiss = onDismiss,
        onPrimaryActionClicked = {
            viewModel.stash(field)
            onDismiss()
        },
    )
}