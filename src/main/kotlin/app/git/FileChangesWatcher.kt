package app.git

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject

private const val MIN_TIME_IN_MS_BETWEEN_REFRESHES = 500L

class FileChangesWatcher @Inject constructor() {
    private var lastNotify = 0L
    private var asyncJob: Job? = null

    private val _changesNotifier = MutableSharedFlow<Long>()
    val changesNotifier: SharedFlow<Long> = _changesNotifier

    suspend fun watchDirectoryPath(pathStr: String, ignoredDirsPath: List<String>) = withContext(Dispatchers.IO) {
        println(ignoredDirsPath)

        val watchService = FileSystems.getDefault().newWatchService()

        val path = Paths.get(pathStr)

        path.register(
            watchService,
            ENTRY_CREATE,
            ENTRY_DELETE,
            ENTRY_MODIFY
        )

        // register directory and sub-directories but ignore dirs by gitignore
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val isIgnoredDirectory = ignoredDirsPath.any { "$pathStr/$it" == dir.toString() }

                return if (!isIgnoredDirectory) {
                    dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                    FileVisitResult.CONTINUE
                } else {
                    FileVisitResult.SKIP_SUBTREE
                }
            }
        })

        var key: WatchKey
        while (watchService.take().also { key = it } != null) {
            key.pollEvents()

            println("Polled events")

            asyncJob?.cancel()

            // Sometimes external apps can run filesystem multiple operations in a fraction of a second.
            // To prevent excessive updates, we add a slight delay between updates emission to prevent slowing down
            // the app by constantly running "git status".
            val currentTimeMillis = System.currentTimeMillis()
            val diffTime = currentTimeMillis - lastNotify

            if (diffTime > MIN_TIME_IN_MS_BETWEEN_REFRESHES) {
                _changesNotifier.emit(currentTimeMillis)
                println("Sync emit with diff time $diffTime")
            } else {
                asyncJob = async {
                    delay(MIN_TIME_IN_MS_BETWEEN_REFRESHES)
                    println("Async emit")
                    if (isActive)
                        _changesNotifier.emit(currentTimeMillis)
                }
            }

            lastNotify = currentTimeMillis

            key.reset()
        }
    }
}