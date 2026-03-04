package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.models.Notification
import com.jetpackduba.gitnuro.domain.repositories.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class InMemoryNotificationsRepository @Inject constructor() : NotificationsRepository {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())

    override val notifications: StateFlow<List<Notification>> = _notifications

    override suspend fun emitNotification(notification: Notification) {
        _notifications.value += notification
    }
}