package com.jetpackduba.gitnuro.ui.context_menu

fun pullContextMenuItems(
    onPullWith: () -> Unit,
    onFetchAll: () -> Unit,
    isPullWithRebaseDefault: Boolean,
): List<ContextMenuElement> {
    val pullWithText = if (isPullWithRebaseDefault) {
        "Pull with merge"
    } else {
        "Pull with rebase"
    }

    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            label = pullWithText,
            onClick = onPullWith,
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Fetch all",
            onClick = onFetchAll,
        ),
    )
}
