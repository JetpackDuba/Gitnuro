package com.jetpackduba.gitnuro.domain.models

sealed interface WatcherEvent {
    data class RepositoryChanged(val hasGitDirChanged: Boolean) : WatcherEvent
    data class WatchInitError(val code: Int) : WatcherEvent
}