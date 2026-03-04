package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.repositories.ClipboardRepository
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Inject

class SkikoClipboardRepository @Inject constructor(
    private val clipboardManager: ClipboardManager
): ClipboardRepository {
    override fun copy(content: String) {
        clipboardManager.setText(content)
    }
}