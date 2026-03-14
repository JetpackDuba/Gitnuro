package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.printDebug
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.data.repositories.SelectedDiffItemRepository
import com.jetpackduba.gitnuro.domain.exceptions.codeToMessage
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import com.jetpackduba.gitnuro.domain.interfaces.IGetWorkspacePathGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.interfaces.IStashChangesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IStageUntrackedFileGitAction
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.IErrorsRepository
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.StashChangesUseCase
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.observers.DataObserversManager
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.OpenUrlInBrowserGitAction
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.ui.IVerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.ui.VerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.diff.DiffViewModel
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import java.awt.Desktop
import javax.inject.Inject
import javax.inject.Provider

private const val MIN_TIME_AFTER_GIT_OPERATION = 2000L

private const val TAG = "TabViewModel"

/**
 * Contains all the information related to a tab and its subcomponents (smaller composables like the log, branches,
 * commit changes, etc.). It holds a reference to every view model because this class lives as long as the tab is open (survives
 * across full app recompositions), therefore, tab's content can be recreated with these view models.
 */
class RepositoryOpenViewModel @Inject constructor(
    private val getWorkspacePathGitAction: IGetWorkspacePathGitAction,
    val diffViewModel: DiffViewModel,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    private val tabState: TabInstanceRepository,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: IFileChangesWatcher,
    private val getAuthorInfoGitAction: GetAuthorInfoGitAction,
    private val stashChangesUseCase: StashChangesUseCase,
    private val openFilePickerGitAction: OpenFilePickerGitAction,
    private val openUrlInBrowserGitAction: OpenUrlInBrowserGitAction,
    private val tabsManager: TabsManager,
    private val tabScope: CoroutineScope,
    private val verticalSplitPaneConfig: VerticalSplitPaneConfig,
    val tabViewModelsProvider: ViewModelsProvider,
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    private val selectedDiffItemRepository: SelectedDiffItemRepository,
    private val dataObserversManager: DataObserversManager,
    sharedRepositoryStateManager: SharedRepositoryStateManager,
    updatesRepository: UpdatesRepository,
) : IVerticalSplitPaneConfig by verticalSplitPaneConfig,
    IGlobalMenuActionsViewModel by globalMenuActionsViewModel,
    TabViewModel() {
    private val errorsManager: IErrorsRepository = tabState.errorsRepository

    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    val repositoryState: StateFlow<RepositoryState> = sharedRepositoryStateManager.repositoryState
    val rebaseInteractiveState: StateFlow<RebaseInteractiveState> = sharedRepositoryStateManager.rebaseInteractiveState

    private val _blameState = MutableStateFlow<BlameState>(BlameState.None)
    val blameState: StateFlow<BlameState> = _blameState

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory

    private val _authorInfoSimple = MutableStateFlow(AuthorInfoSimple(null, null))
    val authorInfoSimple: StateFlow<AuthorInfoSimple> = _authorInfoSimple

    var historyViewModel: HistoryViewModel? = null
        private set

    val diffSelected = selectedDiffItemRepository.diffSelected

    private var hasGitDirChanged = false

    init {
        tabScope.run {
            launch {
                tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.REPO_STATE) {
                    loadAuthorInfo(tabState.git)
                }
            }

            launch {
                watchRepositoryChanges(tabState.git)
            }

            launch {
                dataObserversManager.start()
            }

            launch {
                selectedDiffItemRepository.diffSelected.collectLatest {
                    if (it != null && it.entries.count() == 1) {
                        minimizeBlame()
                    }
                }
            }

            launch {
                loadAuthorInfo(tabState.git)
            }
        }
    }

    override fun onClear() {
        dataObserversManager.stop()
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
        tabsManager.addNewTabFromPath(directory, true, getWorkspacePathGitAction(git))
    }

    private suspend fun loadAuthorInfo(git: Git) {
        _authorInfoSimple.value = getAuthorInfoGitAction(git)
    }

    /**
     * Sometimes external apps can run filesystem multiple operations in a fraction of a second.
     * To prevent excessive updates, we add a slight delay between updates emission to prevent slowing down
     * the app by constantly running "git status" or even full refreshes.
     *
     */
    private suspend fun watchRepositoryChanges(git: Git) = tabScope.launch(Dispatchers.IO) {
        launch {
            fileChangesWatcher.changesNotifier.collect { watcherEvent ->
                when (watcherEvent) {
                    is WatcherEvent.RepositoryChanged -> repositoryChanged(watcherEvent.hasGitDirChanged)
                    is WatcherEvent.WatchInitError -> {
                        val message = codeToMessage(watcherEvent.code)
                        errorsManager.addError(
                            newErrorNow(
                                exception = Exception(message),
                                taskType = TaskType.CHANGES_DETECTION,
                            ),
                        )

                    }
                }
            }
        }

        fileChangesWatcher.watchDirectoryPath(git.repository)
    }

    private suspend fun CoroutineScope.repositoryChanged(hasGitDirChanged: Boolean) {
        val isOperationRunning = tabState.operationRunning

        if (!isOperationRunning) { // Only update if there isn't any process running
            printDebug(TAG, "Detected changes in the repository's directory")

            val currentTimeMillis = System.currentTimeMillis()

            if (
                hasGitDirChanged &&
                currentTimeMillis - tabState.lastOperation < MIN_TIME_AFTER_GIT_OPERATION
            ) {
                printDebug(TAG, "Git operation was executed recently, ignoring file system change")
                return
            }

            if (hasGitDirChanged) {
                this@RepositoryOpenViewModel.hasGitDirChanged = true
            }

            if (isActive) {
                updateApp(hasGitDirChanged)
            }

            this@RepositoryOpenViewModel.hasGitDirChanged = false
        } else {
            printDebug(TAG, "Ignored file events during operation")
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
        tabState.refreshData(RefreshType.UNCOMMITTED_CHANGES_AND_LOG)
    }

    private suspend fun refreshRepositoryInfo() {
        tabState.refreshData(RefreshType.ALL_DATA)
    }

    fun openDirectoryPicker(): String? {
        val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath

        return openFilePickerGitAction(PickerType.DIRECTORIES, latestDirectoryOpened)
    }

    val update: StateFlow<Update?> = updatesRepository.hasUpdatesFlow

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

    fun stashWithMessage(message: String) = stashChangesUseCase(message)

    fun openFolderInFileExplorer() = tabState.runOperation(
        showError = true,
        refreshType = RefreshType.NONE,
    ) { git ->
        Desktop.getDesktop().open(git.repository.workTree)
    }

    fun openUrlInBrowser(url: String) {
        openUrlInBrowserGitAction(url)
    }

    fun closeLastView() = tabScope.launch {
        tabState.closeLastView()
    }
}


sealed interface BlameState {
    data class Loading(val filePath: String) : BlameState

    data class Loaded(val filePath: String, val blameResult: BlameResult, val isMinimized: Boolean = false) : BlameState

    data object None : BlameState
}