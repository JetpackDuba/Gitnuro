package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.branch
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import com.jetpackduba.gitnuro.viewmodels.RenameBranchDialogViewModel
import org.eclipse.jgit.lib.Ref
import org.jetbrains.compose.resources.painterResource

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

    LaunchedEffect(branch) {
        viewModel.operationCompleted.collect { completed ->
            if (completed) {
                onDismiss()
            }
        }
    }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.branch),
        title = "Rename branch",
        subtitle = "Set a new name to the branch \"${branch.simpleName}\"",
        value = field,
        onValueChange = {
            field = it
        },
        primaryActionText = "Rename branch",
        isPrimaryActionEnabled = field.text.isNotBlank(),
        onDismiss = onDismiss,
        onPrimaryActionClicked = {
            viewModel.renameBranch(branch, field.text)
        },
    )
}
