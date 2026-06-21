package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.FileChanged
import com.jetpackduba.gitnuro.FileWatcher
import com.jetpackduba.gitnuro.WatchDirectoryNotifier
import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import com.jetpackduba.gitnuro.domain.models.WatcherEvent
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import javax.inject.Inject

private const val TAG = "FileChangesWatcher"

@TabScope
class FileChangesWatcher @Inject constructor() : AutoCloseable, IFileChangesWatcher {
    private val fileWatcher = FileWatcher()
    private var shouldKeepLooping = true

    init {
        // TODO add error handling
        fileWatcher.init()
    }

    override fun addPathToWatch(path: String, isRecursive: Boolean) {
        fileWatcher.addWatch(path, isRecursive)
    }

    override fun removePathFromWatch(path: String) {
        fileWatcher.removeWatch(path)
    }

    override suspend fun observeEvents(): Flow<WatcherEvent> = callbackFlow {
        fileWatcher.watch(
            notifier = object : WatchDirectoryNotifier {
                override fun shouldKeepLooping(): Boolean = coroutineContext.isActive && shouldKeepLooping
                override fun detectedChange(paths: List<FileChanged>) {
                    trySendBlocking(WatcherEvent.ChangesDetected(paths))
                }

                override fun onError(code: Int) {
                    trySendBlocking(WatcherEvent.WatchInitError(code))
                }
            }
        )
    }


    override fun close() {
        shouldKeepLooping = false
        fileWatcher.close()
    }
}
