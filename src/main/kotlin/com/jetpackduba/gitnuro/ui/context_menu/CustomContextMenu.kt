package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import com.jetpackduba.gitnuro.logging.printError

private const val TAG = "CustomContextMenu"

/**
 * This TextContextMenu will update the parent composable via @param onIsTextSelected when the text selection has changed.
 *
 * An example is the Diff screen, where lines can show different context menus depending on if text is selected or not.
 * If nothing is selected, the default TextContentMenu should not be displayed and the parent composable can decide to
 * show a context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
class SelectionAwareTextContextMenu(val onIsTextSelected: (AnnotatedString) -> Unit) : TextContextMenu {
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

        val emptyTextManager = object : TextContextMenu.TextManager {
            override val copy: (() -> Unit)?
                get() = null
            override val cut: (() -> Unit)?
                get() = null
            override val paste: (() -> Unit)?
                get() = null
            override val selectAll: (() -> Unit)?
                get() = null
            override val selectedText: AnnotatedString
                get() = AnnotatedString("")

            override fun selectWordAtPositionIfNotAlreadySelected(offset: Offset) {}
        }

        val textManagerToUse =
            try {
                if (textManager.selectedText.isNotEmpty()) {
                    textManager
                } else {
                    emptyTextManager
                }
            } catch (ex: Exception) {
                printError(TAG, "Failed to check text manager to use, using empty text manager", ex)
                emptyTextManager
            }

        AppPopupMenu().Area(textManagerToUse, state, content)
    }
}