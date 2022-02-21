package app.git

import app.credentials.GSessionManager
import app.credentials.HttpCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.*
import java.io.File
import javax.inject.Inject


class RemoteOperationsManager @Inject constructor(
    private val sessionManager: GSessionManager
) {
    private val _cloneStatus = MutableStateFlow<CloneStatus>(CloneStatus.None)
    val cloneStatus: StateFlow<CloneStatus>
        get() = _cloneStatus

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

    suspend fun push(git: Git, force: Boolean) = withContext(Dispatchers.IO) {
        val currentBranchRefSpec = git.repository.fullBranch

        val pushResult = git
            .push()
            .setRefSpecs(RefSpec(currentBranchRefSpec))
            .setForce(force)
            .setPushTags()
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

    suspend fun clone(directory: File, url: String) = withContext(Dispatchers.IO) {
        try {
            _cloneStatus.value = CloneStatus.Cloning(0)

            Git.cloneRepository()
                .setDirectory(directory)
                .setURI(url)
                .setProgressMonitor(object : ProgressMonitor {
                    override fun start(totalTasks: Int) {
                        println("ProgressMonitor Start")
                    }

                    override fun beginTask(title: String?, totalWork: Int) {
                        println("ProgressMonitor Begin task")
                    }

                    override fun update(completed: Int) {
                        println("ProgressMonitor Update $completed")
                        _cloneStatus.value = CloneStatus.Cloning(completed)
                    }

                    override fun endTask() {
                        println("ProgressMonitor End task")
                        _cloneStatus.value = CloneStatus.CheckingOut
                    }

                    override fun isCancelled(): Boolean {
                        return !isActive
                    }

                })
                .setTransportConfigCallback {
                    handleTransportCredentials(it)
                }
                .call()

            _cloneStatus.value = CloneStatus.Completed
        } catch (ex: Exception) {
            _cloneStatus.value = CloneStatus.Fail(ex.localizedMessage)
        }
    }

    fun resetCloneStatus() {
        _cloneStatus.value = CloneStatus.None
    }


}

sealed class CloneStatus {
    object None : CloneStatus()
    data class Cloning(val progress: Int) : CloneStatus()
    object CheckingOut : CloneStatus()
    data class Fail(val reason: String) : CloneStatus()
    object Completed : CloneStatus()
}