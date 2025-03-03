package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.tag
import org.jetbrains.compose.resources.painterResource

fun pushContextMenuItems(
    onPushWithTags: () -> Unit,
    onForcePush: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Push including tags",
            icon = { painterResource(Res.drawable.tag) },
            onClick = onPushWithTags,
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Force push",
            onClick = onForcePush,
        ),
    )
}
