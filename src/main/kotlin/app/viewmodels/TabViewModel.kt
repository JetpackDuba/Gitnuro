package app.viewmodels

import app.AppPreferences
import app.AppStateManager
import app.ErrorsManager
import app.credentials.CredentialsState
import app.credentials.CredentialsStateManager
import app.git.*
import app.newErrorNow
import app.ui.SelectedItem
import app.updates.Update
import app.updates.UpdatesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryState
import java.io.File
import javax.inject.Inject

private const val MIN_TIME_IN_MS_BETWEEN_REFRESHES = 1000L

/**
 * Contains all the information related to a tab and its subcomponents (smaller composables like the log, branches,
 * commit changes, etc.). It holds a reference to every view model because this class lives as long as the tab is open (survives
 * across full app recompositions), therefore, tab's content can be recreated with these view models.
 */
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
    val cloneViewModel: CloneViewModel,
    private val repositoryManager: RepositoryManager,
    private val tabState: TabState,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: FileChangesWatcher,
    private val updatesRepository: UpdatesRepository,
) {
    val errorsManager: ErrorsManager = tabState.errorsManager
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    private val credentialsStateManager = CredentialsStateManager

    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    val processing: StateFlow<Boolean> = tabState.processing

    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState

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

    val showError = MutableStateFlow(false)

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

        _repositorySelectionStatus.value = RepositorySelectionStatus.Opening(directory.absolutePath)

        val repository: Repository = repositoryManager.openRepository(directory)

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
            _repositorySelectionStatus.value = RepositorySelectionStatus.None
        }
    }

    private suspend fun loadRepositoryState(git: Git) = withContext(Dispatchers.IO) {
        _repositoryState.value = repositoryManager.getRepositoryState(git)
    }

    private suspend fun watchRepositoryChanges(git: Git) = tabState.managerScope.launch(Dispatchers.IO) {
        val ignored = git.status().call().ignoredNotInIndex.toList()
        var asyncJob: Job? = null
        var lastNotify = 0L
        var hasGitDirChanged = false

        launch {
            fileChangesWatcher.changesNotifier.collect { latestUpdateChangedGitDir ->
                if (!tabState.operationRunning) { // Only update if there isn't any process running
                    println("Detected changes in the repository's directory")

                    if(latestUpdateChangedGitDir) {
                        hasGitDirChanged = true
                    }

                    asyncJob?.cancel()

                    // Sometimes external apps can run filesystem multiple operations in a fraction of a second.
                    // To prevent excessive updates, we add a slight delay between updates emission to prevent slowing down
                    // the app by constantly running "git status".
                    val currentTimeMillis = System.currentTimeMillis()
                    val diffTime = currentTimeMillis - lastNotify

                    // When .git dir has changed, do the refresh with a delay to avoid doing operations while a git
                    // operation may be running
                    if (diffTime > MIN_TIME_IN_MS_BETWEEN_REFRESHES && !hasGitDirChanged) {
                        updateApp(false)
                        println("Sync emit with diff time $diffTime")
                    } else {
                        asyncJob = async {
                            delay(MIN_TIME_IN_MS_BETWEEN_REFRESHES)
                            println("Async emit")
                            if (isActive)
                                updateApp(hasGitDirChanged)

                            hasGitDirChanged = false
                        }
                    }

                    lastNotify = currentTimeMillis
                } else {
                    println("Ignoring changed occurred during operation running...")
                }
            }
        }
        fileChangesWatcher.watchDirectoryPath(
            pathStr = git.repository.directory.parent,
            ignoredDirsPath = ignored,
        )
    }

    suspend fun updateApp(hasGitDirChanged: Boolean) {
        if(hasGitDirChanged) {
            println("Changes detected in git directory, full refresh")

            refreshRepositoryInfo()
        } else {
            println("Changes detected, partial refresh")

            checkUncommitedChanges()
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
        loadRepositoryState(git)
        logViewModel.refresh(git)
        branchesViewModel.refresh(git)
        remotesViewModel.refresh(git)
        tagsViewModel.refresh(git)
        statusViewModel.refresh(git)
        stashesViewModel.refresh(git)
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

    private fun updateDiffEntry() {
        val diffSelected = diffSelected.value

        if (diffSelected != null) {
            diffViewModel.updateDiff(diffSelected)
        }
    }

    fun initLocalRepository(dir: String) = tabState.safeProcessingWihoutGit(
        showError = true,
    ) {
        val repoDir = File(dir)
        repositoryManager.initLocalRepo(repoDir)
        openRepository(repoDir)
    }

    suspend fun latestRelease(): Update? = withContext(Dispatchers.IO) {
        try {
            updatesRepository.latestRelease()
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}


sealed class RepositorySelectionStatus {
    object None : RepositorySelectionStatus()
    data class Opening(val path: String) : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}
