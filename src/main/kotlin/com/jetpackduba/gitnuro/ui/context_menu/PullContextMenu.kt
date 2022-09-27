package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun pullContextMenuItems(
    onPullRebase: () -> Unit,
    onFetchAll: () -> Unit,
): List<DropDownContentData> {
    return mutableListOf(
        DropDownContentData(
            label = "Pull with rebase",
            onClick = onPullRebase,
        ),
        DropDownContentData(
            label = "Fetch all",
            onClick = onFetchAll,
        ),
    )
}
