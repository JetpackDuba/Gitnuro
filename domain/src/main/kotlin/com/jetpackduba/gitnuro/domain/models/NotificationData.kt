package com.jetpackduba.gitnuro.domain.models


//data class Notification(val type: NotificationType, val text: String)
data class NotificationData(val type: NotificationType, val content: String)

enum class NotificationType {
    Warning,
    Positive,
    Error,
}

fun positiveNotification(text: String) = NotificationData(NotificationType.Positive, text)
fun errorNotification(text: String) = NotificationData(NotificationType.Error, text)
fun warningNotification(text: String) = NotificationData(NotificationType.Warning, text)
