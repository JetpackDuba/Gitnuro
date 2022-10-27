package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi

fun pullContextMenuItems(
    onPullRebase: () -> Unit,
): List<DropDownContentData> {
    return mutableListOf(
        DropDownContentData(
            label = "Pull with rebase",
            onClick = onPullRebase,
        ),
    )
}
