package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.tag
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun NewTagDialog(
    onDismiss: () -> Unit,
    onAccept: (tagName: String) -> Unit,
) {
    var field by remember { mutableStateOf("") }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.tag),
        title = "New tag",
        subtitle = "Create a new tag on the specified commit",
        value = field,
        onValueChange = {
            field = it
        },
        primaryActionText = "Create tag",
        isPrimaryActionEnabled = field.isNotBlank(),
        onDismiss = onDismiss,
        onPrimaryActionClicked = { onAccept(field) },
    )
}