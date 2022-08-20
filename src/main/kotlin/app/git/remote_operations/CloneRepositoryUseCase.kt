package app.git.remote_operations

import app.git.CloneStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File
import javax.inject.Inject

class CloneRepositoryUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(directory: File, url: String): Flow<CloneStatus> = callbackFlow {
        var lastTitle: String = ""
        var lastTotalWork = 0
        var progress = 0

        try {
            ensureActive()
            trySend(CloneStatus.Cloning("Starting...", progress, lastTotalWork))

            Git.cloneRepository()
                .setDirectory(directory)
                .setURI(url)
                .setProgressMonitor(
                    object : ProgressMonitor {
                        override fun start(totalTasks: Int) {
                            println("ProgressMonitor Start with total tasks of: $totalTasks")
                        }

                        override fun beginTask(title: String?, totalWork: Int) {
                            println("ProgressMonitor Begin task with title: $title")
                            lastTitle = title.orEmpty()
                            lastTotalWork = totalWork
                            progress = 0
                            trySend(CloneStatus.Cloning(lastTitle, progress, lastTotalWork))
                        }

                        override fun update(completed: Int) {
                            println("ProgressMonitor Update $completed")
                            ensureActive()

                            progress += completed
                            trySend(CloneStatus.Cloning(lastTitle, progress, lastTotalWork))
                        }

                        override fun endTask() {
                            println("ProgressMonitor End task")
                        }

                        override fun isCancelled(): Boolean {
                            return !isActive
                        }
                    }
                )
                .setTransportConfigCallback { handleTransportUseCase(it) }
                .call()

            ensureActive()
            trySend(CloneStatus.Completed(directory))
            channel.close()
        } catch (ex: Exception) {
            if (ex.cause?.cause is CancellationException) {
                println("Clone cancelled")
            } else {
                trySend(CloneStatus.Fail(ex.localizedMessage))
            }

            channel.close()
        }

        awaitClose()
    }
}