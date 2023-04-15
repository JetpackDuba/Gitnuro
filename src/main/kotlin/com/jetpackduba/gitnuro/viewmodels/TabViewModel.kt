package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.AppStateManager
import com.jetpackduba.gitnuro.ErrorsManager
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.git.*
import com.jetpackduba.gitnuro.git.branches.CreateBranchUseCase
import com.jetpackduba.gitnuro.git.rebase.AbortRebaseUseCase
import com.jetpackduba.gitnuro.git.repository.GetRepositoryStateUseCase
import com.jetpackduba.gitnuro.git.repository.InitLocalRepositoryUseCase
import com.jetpackduba.gitnuro.git.repository.OpenRepositoryUseCase
import com.jetpackduba.gitnuro.git.repository.OpenSubmoduleRepositoryUseCase
import com.jetpackduba.gitnuro.git.stash.StashChangesUseCase
import com.jetpackduba.gitnuro.git.workspace.StageUntrackedFileUseCase
import com.jetpackduba.gitnuro.logging.printDebug
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.models.AuthorInfoSimple
import com.jetpackduba.gitnuro.newErrorNow
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import java.awt.Desktop
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
    private val getRepositoryStateUseCase: GetRepositoryStateUseCase,
    private val initLocalRepositoryUseCase: InitLocalRepositoryUseCase,
    private val openRepositoryUseCase: OpenRepositoryUseCase,
    private val openSubmoduleRepositoryUseCase: OpenSubmoduleRepositoryUseCase,
    private val diffViewModelProvider: Provider<DiffViewModel>,
    private val rebaseInteractiveViewModelProvider: Provider<RebaseInteractiveViewModel>,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val authorViewModelProvider: Provider<AuthorViewModel>,
    private val tabState: TabState,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: FileChangesWatcher,
    private val updatesRepository: UpdatesRepository,
    private val credentialsStateManager: CredentialsStateManager,
    private val createBranchUseCase: CreateBranchUseCase,
    private val stashChangesUseCase: StashChangesUseCase,
    private val stageUntrackedFileUseCase: StageUntrackedFileUseCase,
    private val abortRebaseUseCase: AbortRebaseUseCase,
    private val tabScope: CoroutineScope,
) {
    val errorsManager: ErrorsManager = tabState.errorsManager
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem
    var diffViewModel: DiffViewModel? = null

    var rebaseInteractiveViewModel: RebaseInteractiveViewModel? = null
        private set

    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    val processing: StateFlow<ProcessingState> = tabState.processing

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
        tabScope.run {
            launch {
                tabState.refreshData.collect { refreshType ->
                    when (refreshType) {
                        RefreshType.NONE -> printLog(TAG, "Not refreshing...")
                        RefreshType.REPO_STATE -> refreshRepositoryState()
                        else -> {}
                    }
                }
            }
            launch {
                tabState.taskEvent.collect { taskEvent ->
                    when (taskEvent) {
                        is TaskEvent.RebaseInteractive -> onRebaseInteractive(taskEvent)
                        else -> { /*Nothing to do here*/
                        }
                    }
                }
            }

            launch {
                tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.REPO_STATE)
                {
                    loadRepositoryState(tabState.git)
                }
            }

            launch {
                errorsManager.error.collect {
                    showError.value = true
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

    fun openRepository(directory: String) {
        openRepository(File(directory))
    }

    fun openRepository(directory: File) = tabState.safeProcessingWithoutGit {
        printLog(TAG, "Trying to open repository ${directory.absoluteFile}")

        _repositorySelectionStatus.value = RepositorySelectionStatus.Opening(directory.absolutePath)

        try {
            val repository: Repository = if (directory.listFiles()?.any { it.name == ".git" && it.isFile } == true) {
                openSubmoduleRepositoryUseCase(directory)
            } else {
                openRepositoryUseCase(directory)
            }


            repository.workTree // test if repository is valid
            _repositorySelectionStatus.value = RepositorySelectionStatus.Open(repository)
            val git = Git(repository)
            tabState.initGit(git)

            val path = if (directory.name == ".git") {
                directory.parent
            } else
                directory.absolutePath

            onRepositoryChanged(path)
            tabState.newSelectedItem(selectedItem = SelectedItem.UncommitedChanges)
            newDiffSelected = null
            refreshRepositoryInfo()

            watchRepositoryChanges(git)
        } catch (ex: Exception) {
            ex.printStackTrace()
            errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
            _repositorySelectionStatus.value = RepositorySelectionStatus.None
        }
    }

    private suspend fun loadRepositoryState(git: Git) = withContext(Dispatchers.IO) {
        val newRepoState = getRepositoryStateUseCase(git)
        printLog(TAG, "Refreshing repository state $newRepoState")
        _repositoryState.value = newRepoState

        loadAuthorInfo(git)

        onRepositoryStateChanged(newRepoState)
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

    private suspend fun watchRepositoryChanges(git: Git) = tabScope.launch(Dispatchers.IO) {
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
                        printDebug(TAG, "Sync emit with diff time $diffTime")
                    } else {
                        asyncJob = async {
                            delay(MIN_TIME_IN_MS_BETWEEN_REFRESHES)
                            printDebug(TAG, "Async emit")
                            if (isActive)
                                updateApp(hasGitDirChanged)

                            hasGitDirChanged = false
                        }
                    }

                    lastNotify = currentTimeMillis
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
            printDebug(TAG, "Changes detected in git directory, full refresh")

            refreshRepositoryInfo()
        } else {
            printDebug(TAG, "Changes detected, partial refresh")

            checkUncommitedChanges()
        }
    }

    private suspend fun checkUncommitedChanges() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        updateDiffEntry()
        tabState.refreshData(RefreshType.UNCOMMITED_CHANGES_AND_LOG)
//
//        // Stashes list should only be updated if we are doing a stash operation, however it's a small operation
//        // that we can afford to do when doing other operations
//        stashesViewModel.refresh(git)
//        loadRepositoryState(git)
    }

    private suspend fun refreshRepositoryInfo() {
        tabState.refreshData(RefreshType.ALL_DATA)
    }

    fun credentialsDenied() {
        credentialsStateManager.updateState(CredentialsState.CredentialsDenied)
    }

    fun httpCredentialsAccepted(user: String, password: String) {
        credentialsStateManager.updateState(CredentialsAccepted.HttpCredentialsAccepted(user, password))
    }

    fun sshCredentialsAccepted(password: String) {
        credentialsStateManager.updateState(CredentialsAccepted.SshCredentialsAccepted(password))
    }

    var onRepositoryChanged: (path: String) -> Unit = {}

    fun dispose() {
        tabScope.cancel()
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

    fun initLocalRepository(dir: String) = tabState.safeProcessingWithoutGit(
        showError = true,
    ) {
        val repoDir = File(dir)
        initLocalRepositoryUseCase(repoDir)
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

    fun selectCommit(commit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        tabState.newSelectedItem(SelectedItem.Commit(commit))
    }

    fun selectUncommitedChanges() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        tabState.newSelectedItem(SelectedItem.UncommitedChanges, true)
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

    fun refreshAll() = tabScope.launch {
        printLog(TAG, "Manual refresh triggered. IS OPERATION RUNNING ${tabState.operationRunning}")
        if (!tabState.operationRunning) {
            refreshRepositoryInfo()
        }
    }

    fun createBranch(branchName: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashesInteractive = { it is CheckoutConflictException },
    ) { git ->
        createBranchUseCase(git, branchName)
    }

    fun stashWithMessage(message: String) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        stageUntrackedFileUseCase(git)
        stashChangesUseCase(git, message)
    }

    fun openFolderInFileExplorer() = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        Desktop.getDesktop().open(git.repository.directory.parentFile)
    }

    fun cancelRebaseInteractive() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        abortRebaseUseCase(git)
        rebaseInteractiveViewModel = null // shouldn't be necessary but just to make sure
    }

    fun gpgCredentialsAccepted(password: String) {
        credentialsStateManager.updateState(CredentialsAccepted.GpgCredentialsAccepted(password))
    }

    fun cancelOngoingTask() {
        tabState.cancelCurrentTask()
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