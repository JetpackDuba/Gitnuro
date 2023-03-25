package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun stashContextMenuItems(
    onStashWithMessage: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Stash with message",
            onClick = onStashWithMessage,
            icon = { painterResource(AppIcons.MESSAGE) }
        ),
    )
}
