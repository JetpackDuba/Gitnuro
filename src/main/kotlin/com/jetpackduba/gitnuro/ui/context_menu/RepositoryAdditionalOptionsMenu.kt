package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi
import com.jetpackduba.gitnuro.AppIcons

@OptIn(ExperimentalFoundationApi::class)
fun repositoryAdditionalOptionsMenu(
    onOpenRepositoryOnFileExplorer: () -> Unit,
): List<DropDownContentData> {
    return mutableListOf(
        DropDownContentData(
            label = "Open repository folder",
            icon = AppIcons.SOURCE,
            onClick = onOpenRepositoryOnFileExplorer,
        ),
    )
}
