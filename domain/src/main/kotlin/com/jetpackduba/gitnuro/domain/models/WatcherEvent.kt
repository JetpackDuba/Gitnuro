package com.jetpackduba.gitnuro.domain.models

sealed interface WatcherEvent {
    data class WatchInitError(val code: Int) : WatcherEvent
    data class ChangesDetected(val changes: Array<String>) : WatcherEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ChangesDetected

            return changes.contentEquals(other.changes)
        }

        override fun hashCode(): Int {
            return changes.contentHashCode()
        }
    }
}