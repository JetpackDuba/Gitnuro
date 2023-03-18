package com.jetpackduba.gitnuro.ui.context_menu

fun pullContextMenuItems(
    onPullWith: () -> Unit,
    onFetchAll: () -> Unit,
    isPullWithRebaseDefault: Boolean,
): List<DropDownContentData> {
    val pullWithText = if (isPullWithRebaseDefault) {
        "Pull with merge"
    } else {
        "Pull with rebase"
    }

    return mutableListOf(
        DropDownContentData(
            label = pullWithText,
            onClick = onPullWith,
        ),
        DropDownContentData(
            label = "Fetch all",
            onClick = onFetchAll,
        ),
    )
}
