package app.git

import app.credentials.GSessionManager
import app.credentials.HttpCredentialsProvider
import app.extensions.remoteName
import app.extensions.simpleName
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.*
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


class RemoteOperationsManager @Inject constructor(
    private val sessionManager: GSessionManager
) {
    suspend fun pull(git: Git, rebase: Boolean) = withContext(Dispatchers.IO) {
        val pullResult = git
            .pull()
            .setTransportConfigCallback {
                handleTransportCredentials(it)
            }
            .setRebase(rebase)
            .setCredentialsProvider(CredentialsProvider.getDefault())
            .call()

        if (!pullResult.isSuccessful) {
            var message = "Pull failed"

            if (rebase) {
                message = when (pullResult.rebaseResult.status) {
                    RebaseResult.Status.UNCOMMITTED_CHANGES -> "The pull with rebase has failed because you have got uncommited changes"
                    RebaseResult.Status.CONFLICTS -> "Pull with rebase has conflicts, fix them to continue"
                    else -> message
                }
            }

            throw Exception(message)
        }
    }

    suspend fun pullFromBranch(git: Git, rebase: Boolean, remoteBranch: Ref) = withContext(Dispatchers.IO) {
        val pullResult = git
            .pull()
            .setTransportConfigCallback {
                handleTransportCredentials(it)
            }
            .setRemote(remoteBranch.remoteName)
            .setRemoteBranchName(remoteBranch.simpleName)
            .setRebase(rebase)
            .setCredentialsProvider(CredentialsProvider.getDefault())
            .call()

        if (!pullResult.isSuccessful) {
            var message = "Pull failed"

            if (rebase) {
                message = when (pullResult.rebaseResult.status) {
                    RebaseResult.Status.UNCOMMITTED_CHANGES -> "The pull with rebase has failed because you have got uncommited changes"
                    RebaseResult.Status.CONFLICTS -> "Pull with rebase has conflicts, fix them to continue"
                    else -> message
                }
            }

            throw Exception(message)
        }
    }

    suspend fun fetchAll(git: Git) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList().call()

        for (remote in remotes) {
            git.fetch()
                .setRemote(remote.name)
                .setRefSpecs(remote.fetchRefSpecs)
                .setTransportConfigCallback {
                    handleTransportCredentials(it)
                }
                .setCredentialsProvider(CredentialsProvider.getDefault())
                .call()
        }
    }

    suspend fun push(git: Git, force: Boolean, pushTags: Boolean) = withContext(Dispatchers.IO) {
        val currentBranchRefSpec = git.repository.fullBranch

        val pushResult = git
            .push()
            .setRefSpecs(RefSpec(currentBranchRefSpec))
            .setForce(force)
            .apply {
                if (pushTags)
                    setPushTags()
            }
            .setTransportConfigCallback {
                handleTransportCredentials(it)
            }
            .call()

        val results =
            pushResult.map { it.remoteUpdates.filter { remoteRefUpdate -> remoteRefUpdate.status.isRejected } }
                .flatten()
        if (results.isNotEmpty()) {
            val error = StringBuilder()

            results.forEach { result ->
                error.append(result.statusMessage)
                error.append("\n")
            }

            throw Exception(error.toString())
        }
    }

    suspend fun pushToBranch(git: Git, force: Boolean, pushTags: Boolean, remoteBranch: Ref) =
        withContext(Dispatchers.IO) {
            val currentBranchRefSpec = git.repository.fullBranch

            val pushResult = git
                .push()
                .setRefSpecs(RefSpec("$currentBranchRefSpec:${remoteBranch.simpleName}"))
                .setRemote(remoteBranch.remoteName)
                .setForce(force)
                .apply {
                    if (pushTags)
                        setPushTags()
                }
                .setTransportConfigCallback {
                    handleTransportCredentials(it)
                }
                .call()

            val results =
                pushResult.map { it.remoteUpdates.filter { remoteRefUpdate -> remoteRefUpdate.status.isRejected } }
                    .flatten()
            if (results.isNotEmpty()) {
                val error = StringBuilder()

                results.forEach { result ->
                    error.append(result.statusMessage)
                    error.append("\n")
                }

                throw Exception(error.toString())
            }
        }

    private fun handleTransportCredentials(transport: Transport?) {
        if (transport is SshTransport) {
            transport.sshSessionFactory = sessionManager.generateSshSessionFactory()
        } else if (transport is HttpTransport) {
            transport.credentialsProvider = HttpCredentialsProvider()
        }
    }

    suspend fun deleteBranch(git: Git, ref: Ref) = withContext(Dispatchers.IO) {
        val branchSplit = ref.name.split("/").toMutableList()
        val remoteName = branchSplit[2] // Remote name
        repeat(3) {
            branchSplit.removeAt(0)
        }

        val branchName = "refs/heads/${branchSplit.joinToString("/")}"

        val refSpec = RefSpec()
            .setSource(null)
            .setDestination(branchName)

        val pushResult = git.push()
            .setTransportConfigCallback {
                handleTransportCredentials(it)
            }
            .setRefSpecs(refSpec)
            .setRemote(remoteName)
            .call()

        val results =
            pushResult.map { it.remoteUpdates.filter { remoteRefUpdate -> remoteRefUpdate.status.isRejected } }
                .flatten()
        if (results.isNotEmpty()) {
            val error = StringBuilder()

            results.forEach { result ->
                error.append(result.statusMessage)
                error.append("\n")
            }

            throw Exception(error.toString())
        }

        git
            .branchDelete()
            .setBranchNames(ref.name)
            .call()
    }

    private val RemoteRefUpdate.Status.isRejected: Boolean
        get() {
            return this == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD ||
                    this == RemoteRefUpdate.Status.REJECTED_NODELETE ||
                    this == RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED ||
                    this == RemoteRefUpdate.Status.REJECTED_OTHER_REASON
        }

    private val RemoteRefUpdate.statusMessage: String
        get() {
            return when (this.status) {
                RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> "Failed to push some refs to ${this.remoteName}. " +
                        "Updates were rejected because the remote contains work that you do not have locally. Pulling changes from remote may help."
                RemoteRefUpdate.Status.REJECTED_NODELETE -> "Could not delete ref because the remote doesn't support deleting refs."
                RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> "Ref rejected, old object id in remote has changed."
                RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> this.message ?: "Push rejected for unknown reasons."
                else -> ""
            }

        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clone(directory: File, url: String): Flow<CloneStatus> = callbackFlow {
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
                .setTransportConfigCallback {
                    handleTransportCredentials(it)
                }
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

sealed class CloneStatus {
    object None : CloneStatus()
    data class Cloning(val taskName: String, val progress: Int, val total: Int) : CloneStatus()
    object Cancelling : CloneStatus()
    data class Fail(val reason: String) : CloneStatus()
    data class Completed(val repoDir: File) : CloneStatus()
}