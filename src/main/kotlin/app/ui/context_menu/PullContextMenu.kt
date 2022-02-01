package app.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
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
