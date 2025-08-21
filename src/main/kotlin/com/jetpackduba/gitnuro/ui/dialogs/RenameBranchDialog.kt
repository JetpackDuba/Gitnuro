package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.branch
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun RenameBranchDialog(
    oldName: String,
    onDismiss: () -> Unit,
    onAccept: (branchName: String) -> Unit,
) {
    var field by remember { mutableStateOf(oldName) }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.branch),
        title = "Rename branch",
        subtitle = "Set a new name to the branch \"$oldName\"",
        value = field,
        onValueChange = {
            field = it
        },
        primaryActionText = "Rename branch",
        isPrimaryActionEnabled = field.isNotBlank(),
        onDismiss = onDismiss,
        onPrimaryActionClicked = { onAccept(field) },
    )
}
