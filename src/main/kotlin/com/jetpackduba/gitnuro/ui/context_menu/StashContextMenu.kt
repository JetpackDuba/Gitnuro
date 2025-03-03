package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.message
import org.jetbrains.compose.resources.painterResource

fun stashContextMenuItems(
    onStashWithMessage: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Stash with message",
            onClick = onStashWithMessage,
            icon = { painterResource(Res.drawable.message) },
        ),
    )
}
