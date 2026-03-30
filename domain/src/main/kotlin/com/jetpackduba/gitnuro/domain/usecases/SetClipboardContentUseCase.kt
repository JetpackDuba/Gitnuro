package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.models.MessageType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.ClipboardRepository
import com.jetpackduba.gitnuro.domain.repositories.NotificationsRepository
import javax.inject.Inject

class SetClipboardContentUseCase @Inject constructor(
    private val clipboardManager: ClipboardRepository,
    private val notificationsRepository: NotificationsRepository,
) {
    suspend operator fun invoke(content: String, notification: MessageType? = null) {
        clipboardManager.copy(content)

        if (notification != null) {
            notificationsRepository.emitNotification(positiveNotification(notification))
        }
    }
}