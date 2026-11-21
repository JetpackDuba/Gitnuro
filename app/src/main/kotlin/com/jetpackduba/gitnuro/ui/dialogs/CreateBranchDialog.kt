package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.branch
import com.jetpackduba.gitnuro.app.generated.resources.create_branch_dialog_action
import com.jetpackduba.gitnuro.app.generated.resources.create_branch_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.create_branch_dialog_title
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateBranchDialog(
    viewModel: CreateBranchViewModel,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf("") }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.branch),
        title = stringResource(Res.string.create_branch_dialog_title),
        subtitle = stringResource(Res.string.create_branch_dialog_subtitle),
        value = field,
        onValueChange = {
            field = it
        },
        primaryActionText = stringResource(Res.string.create_branch_dialog_action),
        isPrimaryActionEnabled = field.isNotBlank(),
        onDismiss = onDismiss,
        onPrimaryActionClicked = {
            viewModel.createBranch(field)
            onDismiss()
        },
    )
}