package app.viewmodels

import app.AppStateManager
import app.ErrorsManager
import app.credentials.CredentialsState
import app.credentials.CredentialsStateManager
import app.git.*
import app.newErrorNow
import app.ui.SelectedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
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
    val menuViewModel: MenuViewModel,
    val stashesViewModel: StashesViewModel,
    val commitChangesViewModel: CommitChangesViewModel,
    private val repositoryManager: RepositoryManager,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val tabState: TabState,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: FileChangesWatcher,
) {
    val errorsManager: ErrorsManager = tabState.errorsManager
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    private val credentialsStateManager = CredentialsStateManager

    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    val processing: StateFlow<Boolean> = tabState.processing

    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState
    val cloneStatus: StateFlow<CloneStatus> = remoteOperationsManager.cloneStatus

    private val _diffSelected = MutableStateFlow<DiffEntryType?>(null)
    val diffSelected: StateFlow<DiffEntryType?> = _diffSelected
    var newDiffSelected: DiffEntryType?
        get() = diffSelected.value
        set(value) {
            _diffSelected.value = value

            updateDiffEntry()
        }

    private val _repositoryState = MutableStateFlow(RepositoryState.SAFE)
    val repositoryState: StateFlow<RepositoryState> = _repositoryState

    init {
        tabState.managerScope.launch {
            tabState.refreshData.collect { refreshType ->
                when (refreshType) {
                    RefreshType.NONE -> println("Not refreshing...")
                    RefreshType.ALL_DATA -> refreshRepositoryInfo()
                    RefreshType.ONLY_LOG -> refreshLog()
                    RefreshType.STASHES -> refreshStashes()
                    RefreshType.UNCOMMITED_CHANGES -> checkUncommitedChanges()
                    RefreshType.UNCOMMITED_CHANGES_AND_LOG -> checkUncommitedChanges(true)
                    RefreshType.REMOTES -> refreshRemotes()
                }
            }
        }
    }

    private fun refreshRemotes() = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) { git ->
        remotesViewModel.refresh(git)
    }

    private fun refreshStashes() = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) { git ->
        stashesViewModel.refresh(git)
    }

    private fun refreshLog() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        logViewModel.refresh(git)
    }

    fun openRepository(directory: String) {
        openRepository(File(directory))
    }

    fun openRepository(directory: File) = tabState.safeProcessingWihoutGit {
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
            val git = Git(repository)
            tabState.git = git

            onRepositoryChanged(repository.directory.parent)
            refreshRepositoryInfo()

            watchRepositoryChanges(git)
        } catch (ex: Exception) {
            ex.printStackTrace()
            onRepositoryChanged(null)
            errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
        }
    }

    private suspend fun loadRepositoryState(git: Git) = withContext(Dispatchers.IO) {
        _repositoryState.value = repositoryManager.getRepositoryState(git)
    }

    private suspend fun watchRepositoryChanges(git: Git) = tabState.managerScope.launch(Dispatchers.IO) {
        val ignored = git.status().call().ignoredNotInIndex.toList()

        fileChangesWatcher.watchDirectoryPath(
            pathStr = git.repository.directory.parent,
            ignoredDirsPath = ignored,
        ).collect {
            if (!tabState.operationRunning) { // Only update if there isn't any process running
                println("Changes detected, loading status")
                checkUncommitedChanges()
            }
        }
    }

    private suspend fun checkUncommitedChanges(fullUpdateLog: Boolean = false) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val uncommitedChangesStateChanged = statusViewModel.updateHasUncommitedChanges(git)

        println("Has uncommitedChangesStateChanged $uncommitedChangesStateChanged")

        // Update the log only if the uncommitedChanges status has changed or requested
        if (uncommitedChangesStateChanged || fullUpdateLog)
            logViewModel.refresh(git)
        else
            logViewModel.refreshUncommitedChanges(git)

        updateDiffEntry()

        // Stashes list should only be updated if we are doing a stash operation, however it's a small operation
        // that we can afford to do when doing other operations
        stashesViewModel.refresh(git)
    }

    private suspend fun refreshRepositoryInfo() = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
    ) { git ->
        logViewModel.refresh(git)
        branchesViewModel.refresh(git)
        remotesViewModel.refresh(git)
        tagsViewModel.refresh(git)
        statusViewModel.refresh(git)
        stashesViewModel.refresh(git)
        loadRepositoryState(git)
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

    var onRepositoryChanged: (path: String?) -> Unit = {}


    fun dispose() {
        tabState.managerScope.cancel()
    }

    fun clone(directory: File, url: String) = tabState.safeProcessingWihoutGit {
        remoteOperationsManager.clone(directory, url)
    }

    private fun updateDiffEntry() {
        val diffSelected = diffSelected.value

        if (diffSelected != null) {
            diffViewModel.updateDiff(diffSelected)
        }
    }
}


sealed class RepositorySelectionStatus {
    object None : RepositorySelectionStatus()
    object Loading : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}
