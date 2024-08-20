package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.exceptions.WatcherInitException
import com.jetpackduba.gitnuro.git.*
import com.jetpackduba.gitnuro.git.branches.CreateBranchUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.git.repository.InitLocalRepositoryUseCase
import com.jetpackduba.gitnuro.git.repository.OpenRepositoryUseCase
import com.jetpackduba.gitnuro.git.repository.OpenSubmoduleRepositoryUseCase
import com.jetpackduba.gitnuro.git.stash.StashChangesUseCase
import com.jetpackduba.gitnuro.git.workspace.StageUntrackedFileUseCase
import com.jetpackduba.gitnuro.logging.printDebug
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.managers.ErrorsManager
import com.jetpackduba.gitnuro.managers.newErrorNow
import com.jetpackduba.gitnuro.models.AuthorInfoSimple
import com.jetpackduba.gitnuro.models.errorNotification
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.system.OpenFilePickerUseCase
import com.jetpackduba.gitnuro.system.OpenUrlInBrowserUseCase
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.ui.IVerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.ui.VerticalSplitPaneConfig
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

private const val MIN_TIME_AFTER_GIT_OPERATION = 2000L

private const val TAG = "TabViewModel"

/**
 * Contains all the information related to a tab and its subcomponents (smaller composables like the log, branches,
 * commit changes, etc.). It holds a reference to every view model because this class lives as long as the tab is open (survives
 * across full app recompositions), therefore, tab's content can be recreated with these view models.
 */
class TabViewModel @Inject constructor(
    private val initLocalRepositoryUseCase: InitLocalRepositoryUseCase,
    private val openRepositoryUseCase: OpenRepositoryUseCase,
    private val openSubmoduleRepositoryUseCase: OpenSubmoduleRepositoryUseCase,
    private val getWorkspacePathUseCase: GetWorkspacePathUseCase,
    private val diffViewModelProvider: Provider<DiffViewModel>,
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
    private val openFilePickerUseCase: OpenFilePickerUseCase,
    private val openUrlInBrowserUseCase: OpenUrlInBrowserUseCase,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
    private val tabsManager: TabsManager,
    private val tabScope: CoroutineScope,
    private val verticalSplitPaneConfig: VerticalSplitPaneConfig,
    val tabViewModelsProvider: TabViewModelsProvider,
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
) : IVerticalSplitPaneConfig by verticalSplitPaneConfig,
    IGlobalMenuActionsViewModel by globalMenuActionsViewModel {
    var initialPath: String? = null // Stores the path that should be opened when the tab is selected
    val errorsManager: ErrorsManager = tabState.errorsManager
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem
    var diffViewModel: DiffViewModel? = null

    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    val repositoryState: StateFlow<RepositoryState> = sharedRepositoryStateManager.repositoryState
    val rebaseInteractiveState: StateFlow<RebaseInteractiveState> = sharedRepositoryStateManager.rebaseInteractiveState

    val processing: StateFlow<ProcessingState> = tabState.processing

    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState

    private val _diffSelected = MutableStateFlow<DiffType?>(null)
    val diffSelected: StateFlow<DiffType?> = _diffSelected

    var newDiffSelected: DiffType?
        get() = diffSelected.value
        set(value) {
            _diffSelected.value = value
            updateDiffEntry()
        }

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
                tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.REPO_STATE) {
                    loadAuthorInfo(tabState.git)
                }
            }

            launch {
                errorsManager.error.collect {
                    showError.value = true
                }
            }
        }
    }


    /**
     * To make sure the tab opens the new repository with a clean state,
     * instead of opening the repo in the same ViewModel we simply create a new tab with a new TabViewModel
     * replacing the current tab
     */
    fun openAnotherRepository(directory: String) = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        tabsManager.addNewTabFromPath(directory, true, getWorkspacePathUseCase(git))
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
            tabState.newSelectedItem(selectedItem = SelectedItem.UncommittedChanges)
            newDiffSelected = null
            refreshRepositoryInfo()

            watchRepositoryChanges(git)
        } catch (ex: Exception) {
            onRepositoryChanged(null)
            ex.printStackTrace()

            errorsManager.addError(
                newErrorNow(
                    taskType = TaskType.REPOSITORY_OPEN,
                    exception = ex
                )
            )
            _repositorySelectionStatus.value = RepositorySelectionStatus.None
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

    /**
     * Sometimes external apps can run filesystem multiple operations in a fraction of a second.
     * To prevent excessive updates, we add a slight delay between updates emission to prevent slowing down
     * the app by constantly running "git status" or even full refreshes.
     *
     */

    private suspend fun watchRepositoryChanges(git: Git) = tabScope.launch(Dispatchers.IO) {
        var hasGitDirChanged = false

        launch {
            fileChangesWatcher.changesNotifier.collect { latestUpdateChangedGitDir ->
                val isOperationRunning = tabState.operationRunning

                if (!isOperationRunning) { // Only update if there isn't any process running
                    printDebug(TAG, "Detected changes in the repository's directory")

                    val currentTimeMillis = System.currentTimeMillis()

                    if (
                        latestUpdateChangedGitDir &&
                        currentTimeMillis - tabState.lastOperation < MIN_TIME_AFTER_GIT_OPERATION
                    ) {
                        printDebug(TAG, "Git operation was executed recently, ignoring file system change")
                        return@collect
                    }

                    if (latestUpdateChangedGitDir) {
                        hasGitDirChanged = true
                    }

                    if (isActive) {
                        updateApp(hasGitDirChanged)
                    }

                    hasGitDirChanged = false
                } else {
                    printDebug(TAG, "Ignored file events during operation")
                }
            }
        }

        try {
            fileChangesWatcher.watchDirectoryPath(
                repository = git.repository,
            )
        } catch (ex: WatcherInitException) {
            val message = ex.message
            if (message != null) {
                errorsManager.addError(
                    newErrorNow(
                        exception = ex,
                        taskType = TaskType.CHANGES_DETECTION,
//                        message = message,
                    ),
                )
            }
        }
    }

    private suspend fun updateApp(hasGitDirChanged: Boolean) {
        if (hasGitDirChanged) {
            printLog(TAG, "Changes detected in git directory, full refresh")

            refreshRepositoryInfo()
        } else {
            printLog(TAG, "Changes detected, partial refresh")

            checkUncommittedChanges()
        }
    }

    private suspend fun checkUncommittedChanges() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        updateDiffEntry()
        tabState.refreshData(RefreshType.UNCOMMITTED_CHANGES_AND_LOG)
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

    var onRepositoryChanged: (path: String?) -> Unit = {}

    fun dispose() {
        fileChangesWatcher.close()
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

    fun openDirectoryPicker(): String? {
        val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath

        return openFilePickerUseCase(PickerType.DIRECTORIES, latestDirectoryOpened)
    }

    fun initLocalRepository(dir: String) = tabState.safeProcessingWithoutGit {
        val repoDir = File(dir)
        initLocalRepositoryUseCase(repoDir)
        openRepository(repoDir)
    }

    val update: StateFlow<Update?> = updatesRepository.hasUpdatesFlow()
        .flowOn(Dispatchers.IO)
        .stateIn(tabScope, started = SharingStarted.Eagerly, null)

    fun blameFile(filePath: String) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
        taskType = TaskType.BLAME_FILE,
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

        null
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
        tabState.newSelectedCommit(commit)
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
        taskType = TaskType.CREATE_BRANCH,
    ) { git ->
        createBranchUseCase(git, branchName)

        positiveNotification("Branch \"${branchName}\" created")
    }

    fun stashWithMessage(message: String) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.STASH,
    ) { git ->
        stageUntrackedFileUseCase(git)

        if (stashChangesUseCase(git, message)) {
            positiveNotification("Changes stashed")
        } else {
            errorNotification("There are no changes to stash")
        }
    }

    fun openFolderInFileExplorer() = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        Desktop.getDesktop().open(git.repository.workTree)
    }

    fun gpgCredentialsAccepted(password: String) {
        credentialsStateManager.updateState(CredentialsAccepted.GpgCredentialsAccepted(password))
    }

    fun cancelOngoingTask() {
        tabState.cancelCurrentTask()
    }

    fun openUrlInBrowser(url: String) {
        openUrlInBrowserUseCase(url)
    }

    fun removeRepositoryFromRecent(repository: String) {
        appStateManager.removeRepositoryFromRecent(repository)
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

    data object None : BlameState
}