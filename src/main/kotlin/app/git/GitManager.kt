package app.git

import app.AppStateManager
import app.app.ErrorsManager
import app.app.newErrorNow
import app.credentials.CredentialsState
import app.credentials.CredentialsStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import javax.inject.Inject


class GitManager @Inject constructor(
    private val statusManager: StatusManager,
    private val logManager: LogManager,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val branchesManager: BranchesManager,
    private val stashManager: StashManager,
    private val diffManager: DiffManager,
    private val tagsManager: TagsManager,
    private val remotesManager: RemotesManager,
    val errorsManager: ErrorsManager,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: FileChangesWatcher,
) {
    val repositoryName: String
        get() = safeGit.repository.directory.parentFile.name

    private val credentialsStateManager = CredentialsStateManager

    private val managerScope = CoroutineScope(SupervisorJob())

    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean>
        get() = _processing

    private val _lastTimeChecked = MutableStateFlow(System.currentTimeMillis())
    val lastTimeChecked: StateFlow<Long>
        get() = _lastTimeChecked

    val stageStatus: StateFlow<StageStatus> = statusManager.stageStatus
    val logStatus: StateFlow<LogStatus> = logManager.logStatus
    val branches: StateFlow<List<Ref>> = branchesManager.branches
    val tags: StateFlow<List<Ref>> = tagsManager.tags
    val currentBranch: StateFlow<String> = branchesManager.currentBranch
    val stashStatus: StateFlow<StashStatus> = stashManager.stashStatus
    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState
    val cloneStatus: StateFlow<CloneStatus> = remoteOperationsManager.cloneStatus
    val remotes: StateFlow<List<RemoteInfo>> = remotesManager.remotes


    private var git: Git? = null

    private val safeGit: Git
        get() {
            val git = this.git
            if (git == null) {
                _repositorySelectionStatus.value = RepositorySelectionStatus.None
                throw CancellationException()
            } else
                return git
        }

    fun openRepository(directory: String) {
        openRepository(File(directory))
    }

    fun openRepository(directory: File) = managerScope.launch(Dispatchers.IO) {
        safeProcessing {
            println("Trying to open repository ${directory.absoluteFile}")

            val gitDirectory = if (directory.name == ".git") {
                directory
            } else {
                val gitDir = File(directory, ".git")
                if (gitDir.exists() && gitDir.isDirectory) {
                    gitDir
                } else
                    directory

            }

            val builder = FileRepositoryBuilder()
            val repository: Repository = builder.setGitDir(gitDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()

            try {
                repository.workTree // test if repository is valid
                _repositorySelectionStatus.value = RepositorySelectionStatus.Open(repository)
                git = Git(repository)

                onRepositoryChanged(repository.directory.parent)
                refreshRepositoryInfo()
                launch {
                    watchRepositoryChanges()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                onRepositoryChanged(null)
                errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
            }
        }
    }

    private suspend fun watchRepositoryChanges() {
        val ignored = safeGit.status().call().ignoredNotInIndex.toList()

        fileChangesWatcher.watchDirectoryPath(
            pathStr = safeGit.repository.directory.parent,
            ignoredDirsPath = ignored,
        ).collect {
            if (!_processing.value) { // Only update if there isn't any process running
                safeProcessing(showError = false) {
                    println("Changes detected, loading status")
                    statusManager.loadHasUncommitedChanges(safeGit)
                    statusManager.loadStatus(safeGit)
                }
            }
        }
    }

    fun loadLog() = managerScope.launch {
        coLoadLog()
    }

    private suspend fun coLoadLog() {
        logManager.loadLog(safeGit)
    }

    suspend fun loadStatus() {
        val hadUncommitedChanges = statusManager.hasUncommitedChanges.value

        statusManager.loadStatus(safeGit)

        val hasNowUncommitedChanges = statusManager.hasUncommitedChanges.value

        // Update the log only if the uncommitedChanges status has changed
        if (hasNowUncommitedChanges != hadUncommitedChanges)
            coLoadLog()
    }

    fun stage(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.stage(safeGit, diffEntry)
    }

    fun unstage(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.unstage(safeGit, diffEntry)
    }

    fun commit(message: String) = managerScope.launch {
        statusManager.commit(safeGit, message)
        refreshRepositoryInfo()
    }

    val hasUncommitedChanges: StateFlow<Boolean>
        get() = statusManager.hasUncommitedChanges

    suspend fun diffFormat(diffEntryType: DiffEntryType): List<String> {
        try {
            return diffManager.diffFormat(safeGit, diffEntryType)
        } catch (ex: Exception) {
            ex.printStackTrace()
            loadStatus()
            return listOf()
        }
    }

    fun pull() = managerScope.launch {
        safeProcessing {
            remoteOperationsManager.pull(safeGit)
            coLoadLog()
        }
    }

    fun push() = managerScope.launch {
        safeProcessing {
            remoteOperationsManager.push(safeGit)
            coLoadLog()
        }
    }

    private suspend fun refreshRepositoryInfo() {
        statusManager.loadHasUncommitedChanges(safeGit)
        branchesManager.loadBranches(safeGit)
        remotesManager.loadRemotes(safeGit, branchesManager.remoteBranches(safeGit))
        tagsManager.loadTags(safeGit)
        stashManager.loadStashList(safeGit)
        coLoadLog()
    }

    fun stash() = managerScope.launch {
        safeProcessing {
            stashManager.stash(safeGit)
            loadStatus()
            loadLog()
        }
    }

    fun popStash() = managerScope.launch {
        safeProcessing {
            stashManager.popStash(safeGit)
            loadStatus()
            loadLog()
        }
    }

    fun createBranch(branchName: String) = managerScope.launch {
        safeProcessing {
            branchesManager.createBranch(safeGit, branchName)
            coLoadLog()
        }
    }

    fun deleteBranch(branch: Ref) = managerScope.launch {
        safeProcessing {
            branchesManager.deleteBranch(safeGit, branch)
            refreshRepositoryInfo()
        }
    }

    fun deleteTag(tag: Ref) = managerScope.launch {
        safeProcessing {
            tagsManager.deleteTag(safeGit, tag)
            refreshRepositoryInfo()
        }
    }

    fun resetStaged(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.reset(safeGit, diffEntry, staged = true)
        loadLog()
    }

    fun resetUnstaged(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.reset(safeGit, diffEntry, staged = false)
        loadLog()
    }

    fun statusShouldBeUpdated() {
        _lastTimeChecked.value = System.currentTimeMillis()
    }

    fun credentialsDenied() {
        credentialsStateManager.updateState(CredentialsState.CredentialsDenied)
    }

    fun httpCredentialsAccepted(user: String, password: String) {
        credentialsStateManager.updateState(CredentialsState.HttpCredentialsAccepted(user, password))
    }

    fun sshCredentialsAccepted(password: String) {
        credentialsStateManager.updateState(CredentialsState.SshCredentialsAccepted(password))
    }

    suspend fun diffListFromCommit(commit: RevCommit): List<DiffEntry> {
        return diffManager.commitDiffEntries(safeGit, commit)
    }

    fun unstageAll() = managerScope.launch {
        statusManager.unstageAll(safeGit)
    }

    fun stageAll() = managerScope.launch {
        statusManager.stageAll(safeGit)
    }

    fun checkoutCommit(revCommit: RevCommit) = managerScope.launch {
        safeProcessing {
            logManager.checkoutCommit(safeGit, revCommit)
            refreshRepositoryInfo()
        }
    }

    fun revertCommit(revCommit: RevCommit) = managerScope.launch {
        safeProcessing {
            logManager.revertCommit(safeGit, revCommit)
            refreshRepositoryInfo()
        }
    }

    fun resetToCommit(revCommit: RevCommit, resetType: ResetType) = managerScope.launch {
        safeProcessing {
            logManager.resetToCommit(safeGit, revCommit, resetType = resetType)
            refreshRepositoryInfo()
        }
    }

    fun createBranchOnCommit(branch: String, revCommit: RevCommit) = managerScope.launch {
        safeProcessing {
            branchesManager.createBranchOnCommit(safeGit, branch, revCommit)
            refreshRepositoryInfo()
        }
    }

    fun createTagOnCommit(tag: String, revCommit: RevCommit) = managerScope.launch {
        safeProcessing {
            tagsManager.createTagOnCommit(safeGit, tag, revCommit)
            refreshRepositoryInfo()
        }
    }

    var onRepositoryChanged: (path: String?) -> Unit = {}

    @Synchronized
    private suspend fun safeProcessing(showError: Boolean = true, callback: suspend () -> Unit) {
        _processing.value = true
        try {
            callback()
        } catch (ex: Exception) {
            ex.printStackTrace()

            if (showError)
                errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
        } finally {
            _processing.value = false
        }
    }

    fun checkoutRef(ref: Ref) = managerScope.launch {
        safeProcessing {
            logManager.checkoutRef(safeGit, ref)
            refreshRepositoryInfo()
        }
    }

    fun mergeBranch(ref: Ref, fastForward: Boolean) = managerScope.launch {
        safeProcessing {
            branchesManager.mergeBranch(safeGit, ref, fastForward)
            refreshRepositoryInfo()
        }
    }

    fun dispose() {
        managerScope.cancel()
    }

    fun clone(directory: File, url: String) = managerScope.launch {
        remoteOperationsManager.clone(directory, url)
    }

    fun findCommit(objectId: ObjectId): RevCommit {
        return safeGit.repository.parseCommit(objectId)
    }
}


sealed class RepositorySelectionStatus {
    object None : RepositorySelectionStatus()
    object Loading : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}