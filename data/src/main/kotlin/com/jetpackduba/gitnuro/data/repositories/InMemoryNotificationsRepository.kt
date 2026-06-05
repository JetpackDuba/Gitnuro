package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.models.NotificationData
import com.jetpackduba.gitnuro.domain.repositories.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class InMemoryNotificationsRepository @Inject constructor() : NotificationsRepository {
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())

    override val notifications: StateFlow<List<NotificationData>> = _notifications

    override suspend fun emitNotification(notificationData: NotificationData) {
        _notifications.value += notificationData
    }
}