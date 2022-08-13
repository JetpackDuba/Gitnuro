package app.viewmodels

import app.AppStateManager
import app.ErrorsManager
import app.credentials.CredentialsState
import app.credentials.CredentialsStateManager
import app.git.*
import app.logging.printLog
import app.models.AuthorInfoSimple
import app.newErrorNow
import app.ui.SelectedItem
import app.updates.Update
import app.updates.UpdatesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

private const val MIN_TIME_IN_MS_BETWEEN_REFRESHES = 1000L

private const val TAG = "TabViewModel"

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
    val menuViewModel: MenuViewModel,
    val stashesViewModel: StashesViewModel,
    val commitChangesViewModel: CommitChangesViewModel,
    val cloneViewModel: CloneViewModel,
    private val diffViewModelProvider: Provider<DiffViewModel>,
    private val rebaseInteractiveViewModelProvider: Provider<RebaseInteractiveViewModel>,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val authorViewModelProvider: Provider<AuthorViewModel>,
    private val repositoryManager: RepositoryManager,
    private val tabState: TabState,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: FileChangesWatcher,
    private val updatesRepository: UpdatesRepository,
) {
    val errorsManager: ErrorsManager = tabState.errorsManager
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem
    var diffViewModel: DiffViewModel? = null

    var rebaseInteractiveViewModel: RebaseInteractiveViewModel? = null
        private set

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

    private val _blameState = MutableStateFlow<BlameState>(BlameState.None)
    val blameState: StateFlow<BlameState> = _blameState

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory

    private val _showAuthorInfo = MutableStateFlow(false)
    val showAuthorInfo: StateFlow<Boolean> = _showAuthorInfo

    private val _authorInfoSimple = MutableStateFlow(AuthorInfoSimple(null, null))
    val authorInfoSimple: StateFlow<AuthorInfoSimple> = _authorInfoSimple

    var historyViewModel: HistoryViewModel? = null
        private set

    var authorViewModel: AuthorViewModel? = null
        private set

    val showError = MutableStateFlow(false)

    init {
        tabState.managerScope.run {
            launch {
                tabState.refreshData.collect { refreshType ->
                    when (refreshType) {
                        RefreshType.NONE -> printLog(TAG, "Not refreshing...")
                        RefreshType.ALL_DATA -> refreshRepositoryInfo()
                        RefreshType.REPO_STATE -> refreshRepositoryState()
                        RefreshType.ONLY_LOG -> refreshLog()
                        RefreshType.STASHES -> refreshStashes()
                        RefreshType.UNCOMMITED_CHANGES -> checkUncommitedChanges()
                        RefreshType.UNCOMMITED_CHANGES_AND_LOG -> checkUncommitedChanges(true)
                        RefreshType.REMOTES -> refreshRemotes()
                    }
                }
            }
            launch {
                tabState.taskEvent.collect { taskEvent ->
                    when (taskEvent) {
                        is TaskEvent.RebaseInteractive -> onRebaseInteractive(taskEvent)
                    }
                }
            }
        }
    }

    private fun refreshRepositoryState() = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
    ) { git ->
        loadRepositoryState(git)
    }

    private suspend fun onRebaseInteractive(taskEvent: TaskEvent.RebaseInteractive) {
        rebaseInteractiveViewModel = rebaseInteractiveViewModelProvider.get()
        rebaseInteractiveViewModel?.startRebaseInteractive(taskEvent.revCommit)
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
        printLog(TAG, "Trying to open repository ${directory.absoluteFile}")

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
        val newRepoState = repositoryManager.getRepositoryState(git)
        printLog(TAG, "Refreshing repository state $newRepoState")
        _repositoryState.value = newRepoState

        loadAuthorInfo(git)

        onRepositoryStateChanged(newRepoState)

        if (newRepoState == RepositoryState.REBASING_INTERACTIVE && rebaseInteractiveViewModel == null) {
            rebaseInteractiveViewModel = rebaseInteractiveViewModelProvider.get()
            rebaseInteractiveViewModel?.resumeRebase()
        }
    }

    private fun loadAuthorInfo(git: Git) {
        val config = git.repository.config
        config.load()
        val userName = config.getString("user", null, "name")
        val email = config.getString("user", null, "email")

        _authorInfoSimple.value = AuthorInfoSimple(userName, email)
    }

    fun showAuthorInfoDialog() {
        authorViewModel = authorViewModelProvider.get()
        authorViewModel?.loadAuthorInfo()
        _showAuthorInfo.value = true
    }

    fun closeAuthorInfoDialog() {
        _showAuthorInfo.value = false
        authorViewModel = null
    }

    private fun onRepositoryStateChanged(newRepoState: RepositoryState) {
        if (newRepoState != RepositoryState.REBASING_INTERACTIVE && rebaseInteractiveViewModel != null) {
            rebaseInteractiveViewModel?.cancel()
            rebaseInteractiveViewModel = null
        }
    }

    private suspend fun watchRepositoryChanges(git: Git) = tabState.managerScope.launch(Dispatchers.IO) {
        val ignored = git.status().call().ignoredNotInIndex.toList()
        var asyncJob: Job? = null
        var lastNotify = 0L
        var hasGitDirChanged = false

        launch {
            fileChangesWatcher.changesNotifier.collect { latestUpdateChangedGitDir ->
                if (!tabState.operationRunning) { // Only update if there isn't any process running
                    printLog(TAG, "Detected changes in the repository's directory")

                    if (latestUpdateChangedGitDir) {
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
                        printLog(TAG, "Sync emit with diff time $diffTime")
                    } else {
                        asyncJob = async {
                            delay(MIN_TIME_IN_MS_BETWEEN_REFRESHES)
                            printLog(TAG, "Async emit")
                            if (isActive)
                                updateApp(hasGitDirChanged)

                            hasGitDirChanged = false
                        }
                    }

                    lastNotify = currentTimeMillis
                } else {
                    printLog(TAG, "Ignoring changed occurred during operation running...")
                }
            }
        }
        fileChangesWatcher.watchDirectoryPath(
            pathStr = git.repository.directory.parent,
            ignoredDirsPath = ignored,
        )
    }

    suspend fun updateApp(hasGitDirChanged: Boolean) {
        if (hasGitDirChanged) {
            printLog(TAG, "Changes detected in git directory, full refresh")

            refreshRepositoryInfo()
        } else {
            printLog(TAG, "Changes detected, partial refresh")

            checkUncommitedChanges()
        }
    }

    private suspend fun checkUncommitedChanges(fullUpdateLog: Boolean = false) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val uncommitedChangesStateChanged = statusViewModel.updateHasUncommitedChanges(git)

        printLog(TAG, "Has uncommitedChangesStateChanged $uncommitedChangesStateChanged")

        // Update the log only if the uncommitedChanges status has changed or requested
        if (uncommitedChangesStateChanged || fullUpdateLog)
            logViewModel.refresh(git)
        else
            logViewModel.refreshUncommitedChanges(git)

        updateDiffEntry()

        // Stashes list should only be updated if we are doing a stash operation, however it's a small operation
        // that we can afford to do when doing other operations
        stashesViewModel.refresh(git)
        loadRepositoryState(git)
    }

    private fun refreshRepositoryInfo() = tabState.safeProcessing(
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
            if (diffViewModel == null) { // Initialize the view model if required
                diffViewModel = diffViewModelProvider.get()
            }

            diffViewModel?.cancelRunningJobs()
            diffViewModel?.updateDiff(diffSelected)
        } else {
            diffViewModel?.cancelRunningJobs()
            diffViewModel = null // Free the view model from the memory if not being used.
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

    fun blameFile(filePath: String) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
    ) { git ->
        _blameState.value = BlameState.Loading(filePath)
        try {
            val result = git.blame()
                .setFilePath(filePath)
                .setFollowFileRenames(true)
                .call() ?: throw Exception("File is no longer present in the workspace and can't be blamed")

            _blameState.value = BlameState.Loaded(filePath, result)
        } catch (ex: Exception) {
            resetBlameState()

            throw ex
        }
    }

    fun resetBlameState() {
        _blameState.value = BlameState.None
    }

    fun expandBlame() {
        val blameState = _blameState.value

        if (blameState is BlameState.Loaded && blameState.isMinimized) {
            _blameState.value = blameState.copy(isMinimized = false)
        }
    }

    fun minimizeBlame() {
        val blameState = _blameState.value

        if (blameState is BlameState.Loaded && !blameState.isMinimized) {
            _blameState.value = blameState.copy(isMinimized = true)
        }
    }

    fun selectCommit(commit: RevCommit) {
        tabState.newSelectedItem(SelectedItem.Commit(commit))
    }

    fun fileHistory(filePath: String) {
        historyViewModel = historyViewModelProvider.get()
        historyViewModel?.fileHistory(filePath)
        _showHistory.value = true
    }

    fun closeHistory() {
        _showHistory.value = false
        historyViewModel = null
    }

    fun refreshAll() {
        printLog(TAG, "Manual refresh triggered")
        if (!tabState.operationRunning) {
            refreshRepositoryInfo()
        }
    }
}


sealed class RepositorySelectionStatus {
    object None : RepositorySelectionStatus()
    data class Opening(val path: String) : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}

sealed interface BlameState {
    data class Loading(val filePath: String) : BlameState

    data class Loaded(val filePath: String, val blameResult: BlameResult, val isMinimized: Boolean = false) : BlameState

    object None : BlameState
}