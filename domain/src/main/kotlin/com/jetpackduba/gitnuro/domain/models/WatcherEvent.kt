package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.FileChanged

sealed interface WatcherEvent {
    data class WatchInitError(val code: Int) : WatcherEvent
    data class ChangesDetected(val changes: List<FileChanged>) : WatcherEvent
}