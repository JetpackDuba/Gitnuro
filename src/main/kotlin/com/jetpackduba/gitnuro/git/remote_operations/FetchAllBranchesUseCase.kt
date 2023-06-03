package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.exceptions.FetchException
import com.jetpackduba.gitnuro.exceptions.GitnuroException
import com.jetpackduba.gitnuro.logging.printError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RemoteConfig
import javax.inject.Inject

private const val TAG = "FetchAllBranchesUseCase"

class FetchAllBranchesUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
) {
    suspend operator fun invoke(git: Git) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList().call()
        val errors = mutableListOf<Pair<RemoteConfig, Exception>>()
        for (remote in remotes) {
            try {
                git.fetch()
                    .setRemote(remote.name)
                    .setRefSpecs(remote.fetchRefSpecs)
                    .setRemoveDeletedRefs(true)
                    .setTransportConfigCallback { handleTransportUseCase(it, git) }
                    .setCredentialsProvider(CredentialsProvider.getDefault())
                    .setProgressMonitor(object : ProgressMonitor {
                        override fun start(totalTasks: Int) {}

                        override fun beginTask(title: String?, totalWork: Int) {}

                        override fun update(completed: Int) {}

                        override fun endTask() {}

                        override fun isCancelled(): Boolean = isActive

                        override fun showDuration(enabled: Boolean) {}
                    })
                    .call()
            } catch (ex: Exception) {
                printError(TAG, "Fetch failed for remote ${remote.name} with error ${ex.message}", ex)

                if(ex.message != "Cancelled authentication" && ex !is CancellationException) {
                       errors.add(remote to ex)
                }
            }
        }

        if(errors.isNotEmpty()) {
            val errorText = errors.joinToString("\n") {
                "Fetch failed for remote ${it.first.name}: ${it.second.message}"
            }

            throw FetchException(errorText)
        }
    }
}