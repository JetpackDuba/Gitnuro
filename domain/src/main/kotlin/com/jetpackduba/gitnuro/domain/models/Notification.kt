package com.jetpackduba.gitnuro.domain.models


//data class Notification(val type: NotificationType, val text: String)
data class Notification(val type: NotificationType, val content: MessageType)

enum class NotificationType {
    Warning,
    Positive,
    Error,
}

fun positiveNotification(messageType: MessageType) = Notification(NotificationType.Positive, messageType)
fun errorNotification(messageType: MessageType) = Notification(NotificationType.Error, messageType)
fun warningNotification(messageType: MessageType) = Notification(NotificationType.Warning, messageType)

fun positiveNotification(text: String) = Notification(NotificationType.Positive, MessageType.Generic(text))
fun errorNotification(text: String) = Notification(NotificationType.Error, MessageType.Generic(text))
fun warningNotification(text: String) = Notification(NotificationType.Warning, MessageType.Generic(text))

sealed interface MessageType {
    data class BranchCopied(val name: String) : MessageType
    // TODO This should be removed once the refactor is finished
    data class Generic(val content: String) : MessageType
}