package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.branch
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun CreateBranchDialog(
    viewModel: CreateBranchViewModel,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf("") }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.branch),
        title = "New branch",
        subtitle = "Create a new branch and check it out",
        value = field,
        onValueChange = {
            field = it
        },
        primaryActionText = "Create branch",
        isPrimaryActionEnabled = field.isNotBlank(),
        onDismiss = onDismiss,
        onPrimaryActionClicked = {
            viewModel.createBranch(field)
            onDismiss()
        },
    )
}