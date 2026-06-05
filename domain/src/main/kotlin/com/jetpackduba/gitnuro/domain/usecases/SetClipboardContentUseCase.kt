package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.repositories.ClipboardRepository
import javax.inject.Inject

class SetClipboardContentUseCase @Inject constructor(
    private val clipboardManager: ClipboardRepository,
) {
    operator fun invoke(content: String) {
        clipboardManager.copy(content)
    }
}