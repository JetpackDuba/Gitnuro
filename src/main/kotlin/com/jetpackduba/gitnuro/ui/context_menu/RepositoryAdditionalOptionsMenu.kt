package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun repositoryAdditionalOptionsMenu(
    onOpenRepositoryOnFileExplorer: () -> Unit,
    onForceRepositoryRefresh: () -> Unit,
): List<DropDownContentData> {
    return mutableListOf(
        DropDownContentData(
            label = "Open repository folder",
            icon = "source.svg",
            onClick = onOpenRepositoryOnFileExplorer,
        ),
        DropDownContentData(
            label = "Refresh repository",
            icon = "refresh.svg",
            onClick = onForceRepositoryRefresh,
        ),
    )
}
