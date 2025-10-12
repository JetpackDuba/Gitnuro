package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.runtime.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.stash
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun StashWithMessageDialog(
    onDismiss: () -> Unit,
    onAccept: (stashMessage: String) -> Unit,
) {
    var field by remember { mutableStateOf("") }

    SingleTextFieldDialog(
        icon = painterResource(Res.drawable.stash),
        title = "Stash message",
        subtitle = "Create a new stash with a custom message",
        value = field,
        onValueChange = { field = it },
        isPrimaryActionEnabled = field.isNotBlank(),
        primaryActionText = "Stash",
        onDismiss = onDismiss,
        onPrimaryActionClicked = { onAccept(field) },
    )
}