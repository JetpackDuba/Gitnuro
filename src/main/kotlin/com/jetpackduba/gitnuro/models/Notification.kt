package com.jetpackduba.gitnuro.models


data class Notification(val type: NotificationType, val text: String)

enum class NotificationType {
    Warning,
    Positive,
    Error,
}

fun positiveNotification(text: String) = Notification(NotificationType.Positive, text)
fun errorNotification(text: String) = Notification(NotificationType.Error, text)
fun warningNotification(text: String) = Notification(NotificationType.Warning, text)