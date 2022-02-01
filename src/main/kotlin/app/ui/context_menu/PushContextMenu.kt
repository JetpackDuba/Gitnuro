package app.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun pushContextMenuItems(
    onForcePush: () -> Unit,
): List<DropDownContentData> {
    return mutableListOf(
        DropDownContentData(
            label = "Force push",
            onClick = onForcePush,
        ),
    )
}
