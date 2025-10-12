package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.branch
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun NewBranchDialog(
    onDismiss: () -> Unit,
    onAccept: (branchName: String) -> Unit,
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
        onPrimaryActionClicked = { onAccept(field) },
    )
}