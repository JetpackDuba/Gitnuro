package com.jetpackduba.gitnuro.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import java.awt.datatransfer.StringSelection


@OptIn(ExperimentalComposeUiApi::class)
suspend fun Clipboard.setClipboardText(text: String) {
    setClipEntry(ClipEntry(StringSelection(text)))
}
