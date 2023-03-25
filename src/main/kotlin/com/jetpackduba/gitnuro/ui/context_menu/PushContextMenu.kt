package com.jetpackduba.gitnuro.ui.context_menu

fun pushContextMenuItems(
    onPushWithTags: () -> Unit,
    onForcePush: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = "Push including tags",
            onClick = onPushWithTags,
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Force push",
            onClick = onForcePush,
        ),
    )
}
