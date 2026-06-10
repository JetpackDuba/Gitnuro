package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.FSWatchError
import com.jetpackduba.gitnuro.domain.models.WatcherEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.eclipse.jgit.lib.Repository

interface IFileChangesWatcher {
    fun addPathToWatch(path: String, isRecursive: Boolean)
    fun removePathFromWatch(path: String)

    suspend fun observeEvents(): Flow<WatcherEvent>

    fun close()
}