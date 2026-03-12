package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.WatcherEvent
import kotlinx.coroutines.flow.SharedFlow
import org.eclipse.jgit.lib.Repository

interface IFileChangesWatcher {
    val changesNotifier: SharedFlow<WatcherEvent>

    suspend fun watchDirectoryPath(
        repository: Repository,
    )

    fun close()
}