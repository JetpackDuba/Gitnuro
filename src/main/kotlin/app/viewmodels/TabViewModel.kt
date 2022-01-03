package app.viewmodels

import app.AppStateManager
import app.app.ErrorsManager
import app.app.newErrorNow
import app.credentials.CredentialsState
import app.credentials.CredentialsStateManager
import app.git.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import javax.inject.Inject


class TabViewModel @Inject constructor(
    val logViewModel: LogViewModel,
    val branchesViewModel: BranchesViewModel,
    val tagsViewModel: TagsViewModel,
    val remotesViewModel: RemotesViewModel,
    val statusViewModel: StatusViewModel,
    val diffViewModel: DiffViewModel,
    private val repositoryManager: RepositoryManager,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val stashManager: StashManager,
    private val diffManager: DiffManager,
    private val tabState: TabState,
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

    val stashStatus: StateFlow<StashStatus> = stashManager.stashStatus
    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState
    val cloneStatus: StateFlow<CloneStatus> = remoteOperationsManager.cloneStatus

    private val _diffSelected = MutableStateFlow<DiffEntryType?>(null)
    val diffSelected : StateFlow<DiffEntryType?> = _diffSelected
    var newDiffSelected: DiffEntryType?
        get() = diffSelected.value
        set(value){
            _diffSelected.value = value

            updateDiffEntry()
        }

    private val _repositoryState = MutableStateFlow(RepositoryState.SAFE)
    val repositoryState: StateFlow<RepositoryState> = _repositoryState

    init {
        managerScope.launch {
            tabState.refreshData.collect { refreshType ->
                when (refreshType) {
                    RefreshType.NONE -> println("Not refreshing...")
                    RefreshType.ALL_DATA -> refreshRepositoryInfo()
                    RefreshType.ONLY_LOG -> refreshLog()
                    RefreshType.UNCOMMITED_CHANGES -> checkUncommitedChanges()
                }
            }
        }
    }

    private fun refreshLog() = tabState.runOperation { git ->
        logViewModel.refresh(git)

        return@runOperation RefreshType.NONE
    }

    /**
     * Property that indicates if a git operation is running
     */
    @set:Synchronized private var operationRunning = false

    private val safeGit: Git
        get() {
            val git = this.tabState.git
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
                tabState.git = Git(repository)

                onRepositoryChanged(repository.directory.parent)
                refreshRepositoryInfo()
                launch {
                    watchRepositoryChanges()
                }

                println("AppStateManagerReference $appStateManager")
            } catch (ex: Exception) {
                ex.printStackTrace()
                onRepositoryChanged(null)
                errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
            }
        }
    }

    suspend fun loadRepositoryState(git: Git) = withContext(Dispatchers.IO) {
        _repositoryState.value = repositoryManager.getRepositoryState(git)
    }

    private suspend fun watchRepositoryChanges() {
        val ignored = safeGit.status().call().ignoredNotInIndex.toList()

        fileChangesWatcher.watchDirectoryPath(
            pathStr = safeGit.repository.directory.parent,
            ignoredDirsPath = ignored,
        ).collect {
            if (!operationRunning) { // Only update if there isn't any process running
                safeProcessing(showError = false) {
                    println("Changes detected, loading status")
                    statusViewModel.refresh(safeGit)
                    checkUncommitedChanges()

                    updateDiffEntry()
                }
            }
        }
    }

    private suspend fun loadLog() {
        logViewModel.loadLog(safeGit)
    }

    suspend fun checkUncommitedChanges() {
        val uncommitedChangesStateChanged = statusViewModel.updateHasUncommitedChanges(safeGit)

        // Update the log only if the uncommitedChanges status has changed
        if (uncommitedChangesStateChanged)
            loadLog()

        updateDiffEntry()
    }

    fun pull() = managerScope.launch {
        safeProcessing {
            remoteOperationsManager.pull(safeGit)
            loadLog()
        }
    }

    fun push() = managerScope.launch {
        safeProcessing {
            try {
                remoteOperationsManager.push(safeGit)
            } finally {
                loadLog()
            }
        }
    }

    private suspend fun refreshRepositoryInfo() {
        logViewModel.refresh(safeGit)
        branchesViewModel.refresh(safeGit)
        remotesViewModel.refresh(safeGit)
        tagsViewModel.refresh(safeGit)
        statusViewModel.refresh(safeGit)
        loadRepositoryState(safeGit)

        stashManager.loadStashList(safeGit)
        loadLog()
    }

    fun stash() = managerScope.launch {
        safeProcessing {
            stashManager.stash(safeGit)
            checkUncommitedChanges()
            loadLog()
        }
    }

    fun popStash() = managerScope.launch {
        safeProcessing {
            stashManager.popStash(safeGit)
            checkUncommitedChanges()
            loadLog()
        }
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

    var onRepositoryChanged: (path: String?) -> Unit = {}


    fun dispose() {
        managerScope.cancel()
    }

    fun clone(directory: File, url: String) = managerScope.launch {
        remoteOperationsManager.clone(directory, url)
    }

    fun findCommit(objectId: ObjectId): RevCommit {
        return safeGit.repository.parseCommit(objectId)
    }

    private suspend fun safeProcessing(showError: Boolean = true, callback: suspend () -> Unit) {
        _processing.value = true
        operationRunning = true

        try {
            callback()
        } catch (ex: Exception) {
            ex.printStackTrace()

            if (showError)
                errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
        } finally {
            _processing.value = false
            operationRunning = false
        }
    }

    fun updateDiffEntry() = tabState.runOperation { git ->
        val diffSelected = diffSelected.value

        if(diffSelected != null) {
            diffViewModel.updateDiff(git, diffSelected)
        }

        return@runOperation RefreshType.NONE
    }
}


sealed class RepositorySelectionStatus {
    object None : RepositorySelectionStatus()
    object Loading : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}
