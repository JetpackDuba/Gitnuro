package app.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun stashContextMenuItems(
    onStashWithMessage: () -> Unit,
): List<DropDownContentData> {
    return mutableListOf(
        DropDownContentData(
            label = "Stash with message",
            onClick = onStashWithMessage,
        ),
    )
}
