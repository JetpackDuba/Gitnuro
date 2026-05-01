package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.exceptions.FetchException
import com.jetpackduba.gitnuro.domain.interfaces.IFetchAllRemotesGitAction
import com.jetpackduba.gitnuro.domain.models.Remote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RemoteConfig
import javax.inject.Inject

private const val TAG = "FetchAllBranchesGitAction"

class FetchAllRemotesGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val jgit: JGit,
) : IFetchAllRemotesGitAction {
    override suspend operator fun invoke(repositoryPath: String, specificRemote: Remote?) = jgit.provide(repositoryPath) { git ->
        val allRemotes = git.remoteList().call()
        val matchingRemote = specificRemote?.let {
            allRemotes.firstOrNull {
                it.name == specificRemote.name
            }
        }
        val remotes = if (matchingRemote != null) {
            listOf(matchingRemote)
        } else {
            allRemotes
        }

        val errors = mutableListOf<Pair<RemoteConfig, Exception>>()
        for (remote in remotes) {
            try {
                handleTransportGitAction(repositoryPath) {
                    coroutineScope {
                        git
                            .fetch()
                            .setRemote(remote.name)
                            .setRefSpecs(remote.fetchRefSpecs)
                            .setRemoveDeletedRefs(true)
                            .setTransportConfigCallback { handleTransport(it) }
                            .setCredentialsProvider(CredentialsProvider.getDefault())
                            .setProgressMonitor(object : ProgressMonitor {
                                override fun start(totalTasks: Int) {}

                                override fun beginTask(title: String?, totalWork: Int) {}

                                override fun update(completed: Int) {}

                                override fun endTask() {}

                                override fun isCancelled(): Boolean = !isActive

                                override fun showDuration(enabled: Boolean) {}
                            })
                            .call()
                    }
                }
            } catch (ex: Exception) {
                printError(TAG, "Fetch failed for remote ${remote.name} with error ${ex.message}", ex)

                if (ex.message != "Cancelled authentication" && ex !is CancellationException) {
                    errors.add(remote to ex)
                }
            }
        }

        if (errors.isNotEmpty()) {
            val errorText = errors.joinToString("\n") {
                "Fetch failed for remote ${it.first.name}: ${it.second.message}"
            }

            throw FetchException(errorText)
        }
    }
}