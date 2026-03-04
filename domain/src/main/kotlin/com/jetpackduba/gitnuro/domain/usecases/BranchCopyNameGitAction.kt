package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.models.MessageType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.ClipboardRepository
import com.jetpackduba.gitnuro.domain.repositories.NotificationsRepository
import javax.inject.Inject

class BranchCopyNameUseCase @Inject constructor(
    private val clipboardManager: ClipboardRepository,
    private val notificationsRepository: NotificationsRepository,
) {
    suspend fun invoke(content: String) {
        clipboardManager.copy(content)

        notificationsRepository.emitNotification(positiveNotification(MessageType.BranchCopied(content)))
    }
}