package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun pushContextMenuItems(
    onPushWithTags: () -> Unit,
    onForcePush: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Push including tags",
            icon = { painterResource(AppIcons.TAG) },
            onClick = onPushWithTags,
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Force push",
            onClick = onForcePush,
        ),
    )
}
