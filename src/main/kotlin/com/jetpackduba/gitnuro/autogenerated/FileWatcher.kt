
//package 

class FileWatcher private constructor(val pointer: Long) : AutoCloseable {
    private val resource: NativeResource = thread.addObject(this, pointer, "FileWatcher") { FileWatcherObj.destroy(it) }


    fun watch(
        path: String,
        git_dir_path: String,
        notifier: WatchDirectoryNotifier,
    ) =
        FileWatcherObj.watch(this.pointer, path, git_dir_path, notifier)
    

    fun stopWatching() =
        FileWatcherObj.stopWatching(this.pointer)
    

    override fun close() {
        if (thread.contains(resource)) {
            resource.close()
            thread.remove(resource)
        }  else {
            println("FileWatcher was already closed")
        }
    }

    companion object {
    
    fun new(): FileWatcher =
        FileWatcherObj.new()
    
    }
}

private object FileWatcherObj {
        external fun watch(
        pointer: Long,
        path: String,
        git_dir_path: String,
        notifier: WatchDirectoryNotifier,
    )

    external fun new(): FileWatcher

    external fun stopWatching(
        pointer: Long,
    )

    external fun destroy(pointer: Long)
}
