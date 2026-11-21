package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.branch
import com.jetpackduba.gitnuro.app.generated.resources.rename_branch_dialog_action
import com.jetpackduba.gitnuro.app.generated.resources.rename_branch_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.rename_branch_dialog_title
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import com.jetpackduba.gitnuro.viewmodels.RenameBranchDialogViewModel
import com.jetpackduba.gitnuro.viewmodels.RenameState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RenameBranchDialog(
    viewModel: RenameBranchDialogViewModel,
    onDismiss: () -> Unit,
) {
    val branch = viewModel.branch
    var field by remember(branch) {
        val branchName = branch.simpleName

        mutableStateOf(
            TextFieldValue(
                text = branchName,
                selection = TextRange(0, branchName.count())
            )
        )
    }

    val state by viewModel.renameState.collectAsState()

    LaunchedEffect(state) {
        if (state is RenameState.Success) {
            onDismiss()
        }
    }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.branch),
        title = stringResource(Res.string.rename_branch_dialog_title),
        subtitle = stringResource(Res.string.rename_branch_dialog_subtitle, branch.simpleName),
        value = field,
        enabled = state is RenameState.Waiting || state is RenameState.Failed,
        onValueChange = {
            field = it
        },
        primaryActionText = stringResource(Res.string.rename_branch_dialog_action),
        isPrimaryActionEnabled = field.text.isNotBlank(),
        onDismiss = onDismiss,
        onPrimaryActionClicked = {
            viewModel.renameBranch(branch, field.text)
        },
    )
}
