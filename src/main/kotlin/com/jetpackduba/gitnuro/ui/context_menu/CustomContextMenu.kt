package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalFoundationApi::class)
class CustomTextContextMenu(val onIsTextSelected: (AnnotatedString) -> Unit) : TextContextMenu {
    @Composable
    override fun Area(
        textManager: TextContextMenu.TextManager,
        state: ContextMenuState,
        content: @Composable () -> Unit
    ) {
        try {
            // For some reason, compose crashes internally when calling selectedText the first time Area is composed
            onIsTextSelected(textManager.selectedText)
        } catch (ex: Exception) {
            println("Selected text check failed " + ex.message)
        }

        content()
    }
}