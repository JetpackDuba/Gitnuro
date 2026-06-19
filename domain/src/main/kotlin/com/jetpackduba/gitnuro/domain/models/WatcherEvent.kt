package com.jetpackduba.gitnuro.domain.models

sealed interface WatcherEvent {
    data class WatchInitError(val code: Int) : WatcherEvent
    data class ChangesDetected(val changes: List<String>) : WatcherEvent
}