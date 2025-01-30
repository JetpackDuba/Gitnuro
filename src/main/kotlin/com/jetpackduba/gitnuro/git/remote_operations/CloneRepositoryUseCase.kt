package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.git.CloneState
import com.jetpackduba.gitnuro.git.submodules.InitializeAllSubmodulesUseCase
import com.jetpackduba.gitnuro.logging.printDebug
import com.jetpackduba.gitnuro.logging.printError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File
import javax.inject.Inject

private const val TAG = "CloneRepositoryUseCase"

class CloneRepositoryUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val initializeAllSubmodulesUseCase: InitializeAllSubmodulesUseCase,
) {
    operator fun invoke(directory: File, url: String, cloneSubmodules: Boolean): Flow<CloneState> = callbackFlow {
        var lastTitle: String = ""
        var lastTotalWork = 0
        var progress = 0

        try {
            ensureActive()

            trySend(CloneState.Cloning("Starting...", progress, lastTotalWork))

            handleTransportUseCase(null) {
                Git.cloneRepository()
                    .setDirectory(directory)
                    .setURI(url)
                    .setNoCheckout(true)
                    .setProgressMonitor(
                        object : ProgressMonitor {
                            override fun start(totalTasks: Int) {
                                printDebug(TAG, "ProgressMonitor Start with total tasks of: $totalTasks")
                            }

                            override fun beginTask(title: String?, totalWork: Int) {
                                println("ProgressMonitor Begin task with title: $title")
                                lastTitle = title.orEmpty()
                                lastTotalWork = totalWork
                                progress = 0
                                trySend(CloneState.Cloning(lastTitle, progress, lastTotalWork))
                            }

                            override fun update(completed: Int) {
                                printDebug(TAG, "ProgressMonitor Update $completed")
                                ensureActive()

                                progress += completed
                                trySend(CloneState.Cloning(lastTitle, progress, lastTotalWork))
                            }

                            override fun endTask() {
                                printDebug(TAG, "ProgressMonitor End task")
                            }

                            override fun isCancelled(): Boolean {
                                return !isActive
                            }

                            override fun showDuration(enabled: Boolean) {}
                        }
                    )
                    .setTransportConfigCallback { handleTransport(it) }
                    .setCloneSubmodules(cloneSubmodules)
                    .call()
            }

            val git = Git.open(directory)

            useBuiltinLfs(git.repository) {
                git.checkout()
                    .setName(git.repository.fullBranch)
                    .setForced(true)
                    .call()
            }

            // TODO Test this
            initializeAllSubmodulesUseCase(git)

            ensureActive()
            trySend(CloneState.Completed(directory))
            channel.close()
        } catch (ex: Exception) {
            printError(TAG, ex.localizedMessage, ex)
            if (ex.cause?.cause is CancellationException) {
                printDebug(TAG, "Clone cancelled")
            } else {
                trySend(CloneState.Fail(ex.localizedMessage))
            }

            channel.close()
        }

        awaitClose()
    }
}