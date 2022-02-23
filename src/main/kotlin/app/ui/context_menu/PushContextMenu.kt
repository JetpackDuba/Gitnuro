package app.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun pushContextMenuItems(
    onPushWithTags: () -> Unit,
    onForcePush: () -> Unit,
): List<DropDownContentData> {
    return mutableListOf(
        DropDownContentData(
            label = "Push including tags",
            onClick = onPushWithTags,
        ),
        DropDownContentData(
            label = "Force push",
            onClick = onForcePush,
        ),
    )
}
