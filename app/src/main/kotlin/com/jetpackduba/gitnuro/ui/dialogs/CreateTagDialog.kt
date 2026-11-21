package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.create_tag_dialog_action
import com.jetpackduba.gitnuro.app.generated.resources.create_tag_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.create_tag_dialog_title
import com.jetpackduba.gitnuro.app.generated.resources.tag
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateTagDialog(
    viewModel: CreateTagViewModel,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf("") }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.tag),
        title = stringResource(Res.string.create_tag_dialog_title),
        subtitle = stringResource(Res.string.create_tag_dialog_subtitle),
        value = field,
        onValueChange = {
            field = it
        },
        primaryActionText = stringResource(Res.string.create_tag_dialog_action),
        isPrimaryActionEnabled = field.isNotBlank(),
        onDismiss = onDismiss,
        onPrimaryActionClicked = {
            viewModel.createTag(field)
            onDismiss()
        },
    )
}