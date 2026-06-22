package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.collectLatestInViewModel
import com.jetpackduba.gitnuro.common.flows.invert
import com.jetpackduba.gitnuro.common.printDebug
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.exceptions.InvalidMessageException
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.extensions.toMutableSetAndAdd
import com.jetpackduba.gitnuro.domain.extensions.toMutableSetAndRemove
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.CompletedTask
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.domain.usecases.*
import com.jetpackduba.gitnuro.extensions.stateIn
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.OpenUrlInBrowserGitAction
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.ui.AppViewModel
import com.jetpackduba.gitnuro.ui.IVerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.VerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.status.StatusAction
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import com.jetpackduba.gitnuro.viewmodels.*
import com.jetpackduba.gitnuro.viewmodels.RebaseAction
import com.jetpackduba.gitnuro.viewmodels.RebaseLine
import com.jetpackduba.gitnuro.viewmodels.sidepanel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration.Companion.milliseconds


private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 500L


private const val MIN_TIME_AFTER_GIT_OPERATION = 2000L


/**
 * Represents when the search filter is not being used or the results list is empty
 */
private const val NONE_MATCHING_INDEX = 0

/**
 * The search UI starts the index count at 1 (for example "1/10" to represent the first commit of the search result
 * being selected)
 */
private const val FIRST_INDEX = 1

private const val LOG_MIN_TIME_IN_MS_TO_SHOW_LOAD = 500L

private const val TAG = "TabViewModel"

/**
 * Contains all the information related to a tab and its subcomponents (smaller composables like the log, branches,
 * commit changes, etc.). It holds a reference to every view model because this class lives as long as the tab is open (survives
 * across full app recompositions), therefore, tab's content can be recreated with these view models.
 */
class RepositoryOpenViewModel @Inject constructor(
    private val getWorktreeUseCase: GetWorktreeUseCase,
    private val historyViewModelProvider: Provider<HistoryViewModel>,
    val appStateManager: AppStateManager,
    private val openFilePickerGitAction: OpenFilePickerGitAction,
    private val openUrlInBrowserGitAction: OpenUrlInBrowserGitAction,
    private val appViewModel: AppViewModel,
    private val tabScope: TabCoroutineScope,
    private val verticalSplitPaneConfig: VerticalSplitPaneConfig,
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val blameFileUseCase: BlameFileUseCase,
    updatesRepository: UpdatesRepository,
    private val fetchRemotesUseCase: FetchRemotesUseCase,
    private val deleteRemoteInfoUseCase: DeleteRemoteInfoUseCase,
    private val checkoutCommitUseCase: CheckoutCommitUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val deleteRemoteBranchUseCase: DeleteRemoteBranchUseCase,
    private val deleteSubmoduleUseCase: DeleteSubmoduleUseCase,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val checkoutBranchUseCase: CheckoutBranchUseCase,
    private val updateSubmoduleUseCase: UpdateSubmoduleUseCase,
    private val syncSubmoduleUseCase: SyncSubmoduleUseCase,
    private val pushBranchUseCase: PushBranchUseCase,
    private val pullBranchUseCase: PullBranchUseCase,
    private val initializeSubmoduleUseCase: InitializeSubmoduleUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val applyStashUseCase: ApplyStashUseCase,
    private val popStashUseCase: PopStashUseCase,
    private val deleteStashUseCase: DeleteStashUseCase,
    private val startRebaseInteractiveUseCase: StartRebaseInteractiveUseCase,
    private val cherryPickCommitUseCase: CherryPickCommitUseCase,
    private val revertCommitUseCase: RevertCommitUseCase,
    private val abortRebaseUseCase: AbortRebaseUseCase,
    private val appSettings: AppSettingsService,
    private val addSelectedDiffUseCase: AddSelectedDiffUseCase,
    private val getSummaryFromStatusUseCase: GetSummaryFromStatusUseCase,
    private val stageHunkUseCase: StageHunkUseCase,
    private val unstageHunkUseCase: UnstageHunkUseCase,
    private val stageHunkLineUseCase: StageHunkLineUseCase,
    private val unstageHunkLineUseCase: UnstageHunkLineUseCase,
    private val resetHunkUseCase: ResetHunkUseCase,
    private val discardHunkLineUseCase: DiscardHunkLineUseCase,
    private val statusStageUseCase: StatusStageUseCase,
    private val statusUnstageUseCase: StatusUnstageUseCase,
    private val openFileInExternalAppUseCase: OpenFileInExternalAppUseCase,
    private val getDiffUseCase: GetDiffUseCase,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val settings: AppSettingsService,
//    private val getRebaseLinesFullMessageGitAction: IGetRebaseLinesFullMessageGitAction,
    private val getCommitFromRebaseLineUseCase: GetCommitFromRebaseLineUseCase,
    private val resumeRebaseInteractiveUseCase: ResumeRebaseInteractiveUseCase,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val statusViewModelExtenderFactory: StatusViewModelExtender.Factory,
    private val commitChangesViewModelExtenderFactory: CommitChangesViewModelExtender.Factory,
    private val getCommitFromHashUseCase: GetCommitFromHashUseCase,
) : IVerticalSplitPaneConfig by verticalSplitPaneConfig,
    IGlobalMenuActionsViewModel by globalMenuActionsViewModel,
    TabViewModel() {
    val showAsTree = appSettings.showChangesAsTree
        .stateIn(false)

    val isPullWithRebaseDefault = settings.pullWithRebase

    val lastLoadedTabs = appStateManager.latestOpenedRepositoriesPaths

    val repositoryState: StateFlow<RepositoryState> = repositoryDataRepository.repositoryState
    val rebaseInteractiveState: StateFlow<RebaseInteractiveState> = repositoryDataRepository.rebaseInteractiveState

    val filter: StateFlow<String>
        field = MutableStateFlow("")

    val selectedItem: StateFlow<SelectedItem>
        field = MutableStateFlow<SelectedItem>(SelectedItem.UncommittedChanges)

    val isExpandedBranches: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    val isExpandedRemotes: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(false)

    val isExpandedStashes: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    val isExpandedTags: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(false)

    val isExpandedSubmodules: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    val freeSearchFocusFlow: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>()

    val diffSelected: StateFlow<DiffSelected?>
        field = MutableStateFlow<DiffSelected?>(null)

    private val closeableViews = ArrayDeque<CloseableView>()
    private val closeableViewsMutex = Mutex()

    private val _closeView = MutableSharedFlow<CloseableView>()
    val closeViewFlow = _closeView.asSharedFlow()

    fun addCloseableView(view: CloseableView) {
        viewModelScope.launch {
            closeableViewsMutex.withLock {
                closeableViews.remove(view) // Remove any previous elements if present
                closeableViews.add(view)
            }
        }
    }

    fun removeCloseableView(view: CloseableView) {
        viewModelScope.launch {
            closeableViewsMutex.withLock {
                closeableViews.remove(view)
            }
        }
    }

    fun closeLastView() {
        viewModelScope.launch {
            closeableViewsMutex.withLock {
                val last = closeableViews.removeLastOrNull()

                if (last != null) {
                    _closeView.emit(last)
                }
            }
        }
    }

    private val branches = repositoryDataRepository.localBranches
    private val currentBranch = repositoryDataRepository.currentBranch

    private val logBranchesByCommitHash =
        combine(branches, repositoryDataRepository.remotes, currentBranch) { branches, remotes, currentBranch ->
            (branches + remotes.flatMap { it.branchesList })
                .filter { branch ->
                    currentBranch?.name == "HEAD" || branch.simpleName != "HEAD"
                }
                .groupBy { branch -> branch.hash }
        }
            .distinctUntilChanged()

    private val tagsByCommitHash = repositoryDataRepository.tags.map {
        it.groupBy { tag -> tag.hash }
    }

    private val stashesHashes = repositoryDataRepository.stashes
        .map {
            it
                .map { commit ->
                    commit.hash
                }
                .toHashSet()
        }

    val branchesState =
        combineBranchesState(branches, currentBranch, isExpandedBranches, filter)
            .stateIn(BranchesState(emptyList(), isExpandedBranches.value, null))

    private val remotesContracted = MutableStateFlow<Set<Remote>>(emptySet())
    val remoteState: StateFlow<RemotesState> =
        combineRemotesState(
            repositoryDataRepository.remotes,
            isExpandedRemotes,
            filter,
            currentBranch,
            remotesContracted,
        ).stateIn(RemotesState())

    val stashesState: StateFlow<StashesState> =
        combine(repositoryDataRepository.stashes, isExpandedStashes, filter) { stashes, isExpanded, filter ->
            StashesState(
                stashes = stashes.filter { it.message.lowercaseContains(filter) },
                isExpanded,
            )
        }.stateIn(StashesState(emptyList(), isExpandedStashes.value))

    val tagsState: StateFlow<TagsState> =
        combine(repositoryDataRepository.tags, isExpandedTags, filter) { tags, isExpanded, filter ->
            TagsState(
                tags.filter { tag -> tag.simpleName.lowercaseContains(filter) },
                isExpanded,
            )
        }.stateIn(TagsState(emptyList(), isExpandedTags.value))


    val submodulesState: StateFlow<SubmodulesState> =
        combine(repositoryDataRepository.submodules, isExpandedSubmodules, filter) { submodules, isExpanded, filter ->
            SubmodulesState(
                submodules = submodules.filter { it.key.lowercaseContains(filter) }.toList(),
                isExpanded = isExpanded
            )
        }.stateIn(SubmodulesState(emptyList(), isExpandedSubmodules.value))

    private val hasUncommittedChanges = repositoryDataRepository.status.map {
        it.staged.isNotEmpty() || it.unstaged.isNotEmpty()
    }

    private val log = repositoryDataRepository.log
    private val statusSummary = repositoryDataRepository.status.map {
        getSummaryFromStatusUseCase(it)
    }


    private val verticalListState = MutableStateFlow(LazyListState(0, 0))
    private val horizontalListState = MutableStateFlow(ScrollState(0))


    val logSearchFilterResults: StateFlow<LogSearch>
        field = MutableStateFlow<LogSearch>(LogSearch.NotSearching)

    val logState = combineLogState(
        log,
        hasUncommittedChanges,
        currentBranch,
        branches = logBranchesByCommitHash,
        tags = tagsByCommitHash,
        stashes = stashesHashes,
        statusSummary,
        logSearchFilterResults,
        verticalListState,
        horizontalListState,
    )
        .debounce(LOG_MIN_TIME_IN_MS_TO_SHOW_LOAD.milliseconds)
        .stateIn(LogState(true))


    private val statusViewModelExtender = statusViewModelExtenderFactory.create(
        viewModelScope,
        showAsTree,
        diffSelected,
        rebaseInteractiveState,
        onDiffSelected = {
            diffSelected.value = it
        },
        onRemoveEntriesFromSelection = { entries, entryType ->
            removeSelectedDiff(entries, entryType)
        },
        onAlternateShowAsTree = ::alternateShowAsTree,
        addCloseableView = ::addCloseableView,
        removeCloseableView = ::removeCloseableView,
    )
    private val commitChangesViewModelExtender = commitChangesViewModelExtenderFactory.create(
        viewModelScope,
        showAsTree,
        selectedItem,
        diffSelected,
        onDiffSelected = {
            diffSelected.value = it
        },
        onAlternateShowAsTree = ::alternateShowAsTree,
        addCloseableView = ::addCloseableView,
        removeCloseableView = ::removeCloseableView,
    )

    val commitChangesState = commitChangesViewModelExtender.commitChangesState

    val statusState = statusViewModelExtender.statusState

    init {
        closeViewFlow.collectLatestInViewModel {
            when (it) {
                CloseableView.SIDE_PANE_SEARCH -> {
                    newFilter("")
                    freeSearchFocusFlow.emit(Unit)
                }

                CloseableView.LOG_SEARCH -> {
                    logSearchFilterResults.value = LogSearch.NotSearching
                }

                CloseableView.STAGED_CHANGES_SEARCH -> {
                    statusViewModelExtender.searchFilterToggledStaged(false)
                }

                CloseableView.UNSTAGED_CHANGES_SEARCH -> {
                    statusViewModelExtender.searchFilterToggledUnstaged(false)
                }

                CloseableView.DIFF -> {
                }

                CloseableView.COMMIT_CHANGES_SEARCH -> commitChangesViewModelExtender.searchFilterToggled(false)

            }
        }

        tabScope.run {
            launch {
                //watchRepositoryChanges(tabState.git)
            }
        }

        diffSelected.collectLatestInViewModel {
            if (it != null && it.entries.count() == 1) {
                minimizeBlame()
            }
        }

        logSearchFilterResults.collectLatestInViewModel {
            when (it) {
                LogSearch.NotSearching -> removeSearchFromCloseableView()
                is LogSearch.SearchResults -> addSearchToCloseableView()
            }
        }
    }

    fun newFilter(newValue: String) {
        filter.value = newValue
    }

    fun addSidePanelSearchToCloseables() = tabScope.launch {
        addCloseableView(CloseableView.SIDE_PANE_SEARCH)
    }

    fun removeSidePanelSearchFromCloseables() = tabScope.launch {
        removeCloseableView(CloseableView.SIDE_PANE_SEARCH)
    }

    fun onExpandBranches() {
        isExpandedBranches.invert()
    }

    fun onExpandRemotes() {
        isExpandedRemotes.invert()
    }

    fun onExpandSubmodules() {
        isExpandedSubmodules.invert()
    }

    fun onExpandStashes() {
        isExpandedStashes.invert()
    }

    fun onExpandTags() {
        isExpandedTags.invert()
    }


    fun onRemoteClicked(remoteClicked: RemoteView) {
        remotesContracted.value = if (remotesContracted.value.contains(remoteClicked.remoteInfo.remote)) {
            remotesContracted.value.toMutableSetAndRemove(remoteClicked.remoteInfo.remote)
        } else {
            remotesContracted.value.toMutableSetAndAdd(remoteClicked.remoteInfo.remote)
        }
    }

    fun selectBranch(branch: Branch) = viewModelScope.launch {
        val commit = getCommitFromHashUseCase(branch.hash).okOrNull()

        if (commit != null) {
            selectedItem.value = SelectedItem.BranchItem(branch, commit)
        }
    }

    fun deleteRemote(remoteInfo: RemoteInfo) = deleteRemoteInfoUseCase(remoteInfo)

    fun onFetchRemoteBranches(remote: RemoteView) = fetchRemotesUseCase(remote.remoteInfo.remote)

    fun checkoutTagCommit(tag: Tag) = checkoutCommitUseCase(tag.hash)

    fun selectTag(tag: Tag) = viewModelScope.launch {
        val commit = getCommitFromHashUseCase(tag.hash).okOrNull()

        if (commit != null) {
            selectedItem.value = SelectedItem.TagItem(tag, commit)
        }
    }

    fun onOpenSubmoduleInTab(path: String) = viewModelScope.launch {
        val repositoryPath = repositoryDataRepository.repositoryPath

        if (repositoryPath != null) {
            // TODO Repository path may point to git dir and not workdir? If so, add use case
            appViewModel.addNewTabFromPath("$repositoryPath/$path", true)
        }
    }

    fun initializeSubmodule(path: String) = initializeSubmoduleUseCase(path)

    fun syncSubmodule(path: String) = syncSubmoduleUseCase(path)

    fun updateSubmodule(path: String) = updateSubmoduleUseCase(path)

    fun deleteSubmodule(path: String) = deleteSubmoduleUseCase(path)

    fun mergeBranch(branch: Branch) = mergeBranchUseCase(branch)

    fun deleteBranch(branch: Branch) = deleteBranchUseCase(branch)

    fun checkoutBranch(branch: Branch) = checkoutBranchUseCase(branch)

    fun rebaseBranch(branch: Branch) = rebaseBranchUseCase(branch)

    fun deleteRemoteBranch(branch: Branch) = deleteRemoteBranchUseCase(branch)

    fun checkoutRemoteBranch(remoteBranch: Branch) = checkoutBranchUseCase(remoteBranch)

    fun applyStash(stash: Commit) = applyStashUseCase(stash)
    fun popStash(stash: Commit) = popStashUseCase(stash)
    fun deleteStash(stash: Commit) = deleteStashUseCase(stash)

    fun pushToRemoteBranch(branch: Branch) = pushBranchUseCase(
        force = false,
        pushTags = false,
        targetRemoteBranch = branch
    )

    fun pullFromRemoteBranch(branch: Branch) = pullBranchUseCase(PullType.DEFAULT, branch)

    fun deleteTag(tag: Tag) = deleteTagUseCase(tag)
    fun selectStash(stash: Commit) {
        selectCommit(stash)
    }

    private val _blameState = MutableStateFlow<BlameState>(BlameState.None)
    val blameState: StateFlow<BlameState> = _blameState

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory

    val authorInfoSimple = repositoryDataRepository
        .author
        .map { it.identityToUse() }
        .stateIn(emptyIdentity())

    var historyViewModel: HistoryViewModel? = null
        private set

    private var hasGitDirChanged = false


    override fun onClear() {
    }

    /**
     * To make sure the tab opens the new repository with a clean state,
     * instead of opening the repo in the same ViewModel we simply create a new tab with a new TabViewModel
     * replacing the current tab
     */
    fun openAnotherRepository(directory: String) {
        viewModelScope.launch {
            val worktree = getWorktreeUseCase()

            if (worktree is Either.Ok) {
                appViewModel.addNewTabFromPath(directory, true, worktree.value)
            }
        }
    }

    private suspend fun CoroutineScope.repositoryChanged(hasGitDirChanged: Boolean) {
        val isOperationRunning = repositoryStateRepository.currentTask.value != null

        if (!isOperationRunning) { // Only update if there isn't any process running
            printDebug(TAG, "Detected changes in the repository's directory")

            val currentTimeMillis = System.currentTimeMillis()
            val lastOperationTimestamp = repositoryStateRepository.lastOperationTimestamp.firstOrNull() ?: 0L
            if (
                hasGitDirChanged &&
                currentTimeMillis - lastOperationTimestamp < MIN_TIME_AFTER_GIT_OPERATION
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

    private suspend fun checkUncommittedChanges() {
        refreshStatusUseCase()
        refreshLogUseCase()
    }

    private suspend fun refreshRepositoryInfo() {
        refreshAllUseCase()
    }

    fun openDirectoryPicker(): String? {
        val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath

        return openFilePickerGitAction(PickerType.DIRECTORIES, latestDirectoryOpened)
    }

    val update: StateFlow<Update?> = updatesRepository.hasUpdatesFlow

    fun blameFile(filePath: String) {
        viewModelScope.launch {
            _blameState.value = BlameState.Loading(filePath)

            when (val result = blameFileUseCase(filePath)) {
                is Either.Err -> {
                    resetBlameState()
                }

                is Either.Ok -> {
                    _blameState.value = BlameState.Loaded(filePath, result.value)
                }
            }
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
        val currentTask = repositoryStateRepository.currentTask
        printLog(TAG, "Manual refresh triggered. Current task: $currentTask")

        if (repositoryStateRepository.currentTask.value == null) {
            refreshRepositoryInfo()
        }
    }

    fun openUrlInBrowser(url: String) {
        openUrlInBrowserGitAction(url)
    }

    var savedSearchFilter: String = ""
    var graphPadding = 0f

    private var lastIndexUsedToLoadData = 0
    private val loadItemsMutex = Mutex()

    // TODO Restore functionality after refactoring
    private val scrollToItem: Flow<GraphCommit> = emptyFlow() /*tabState.taskEvent
        .filterIsInstance<TaskEvent.ScrollToGraphItem>()
        .map { it.selectedItem }
        .filterIsInstance<SelectedItem.CommitBasedItem>()
        .map { it.revCommit }
*/
    val scrollToUncommittedChanges: Flow<SelectedItem.UncommittedChanges> = emptyFlow() /*tabState.taskEvent
        .filterIsInstance<TaskEvent.ScrollToGraphItem>()
        .map { it.selectedItem }
        .filterIsInstance()*/

    private val _focusCommit = MutableSharedFlow<GraphCommit>()
    val focusCommit: Flow<GraphCommit> = merge(_focusCommit, scrollToItem)


    fun onAction(action: StatusAction) {
        statusViewModelExtender.onAction(action)
    }

    fun onAction(action: CommitChangesAction) {
        commitChangesViewModelExtender.onAction(action)
    }

    fun onAction(action: LogAction) {
        when (action) {
            is LogAction.ApplyStash -> applyStash(action.commit)
            is LogAction.CheckoutCommit -> checkoutCommit(action.commit)
            is LogAction.CheckoutBranch -> checkoutBranch(action.branch)
            is LogAction.CheckoutRemoteBranch -> checkoutRemoteBranch(action.branch)
            is LogAction.CheckoutTag -> checkoutTag(action.tag)
            is LogAction.CherryPickCommit -> cherryPickCommit(action.commit)
            is LogAction.CommitSelected -> selectCommit(action.commit)
            is LogAction.DeleteBranch -> deleteBranch(action.branch)
            is LogAction.DeleteRemoteBranch -> deleteRemoteBranch(action.branch)
            is LogAction.DeleteStash -> deleteStash(action.commit)
            is LogAction.DeleteTag -> deleteTag(action.tag)
            is LogAction.Merge -> mergeBranch(action.branch)
            is LogAction.PopStash -> popStash(action.commit)
            is LogAction.PullFromRemoteBranch -> pullFromRemoteBranch(action.branch)
            is LogAction.PushToRemoteBranch -> pushToRemoteBranch(action.branch)
            is LogAction.Rebase -> rebaseBranch(action.branch)
            is LogAction.RebaseInteractive -> rebaseInteractive(action.commit)
            is LogAction.RevertCommit -> revertCommit(action.commit)
            LogAction.UncommittedChangesSelected -> selectUncommittedChanges()
            is LogAction.SearchValueChange -> onSearchValueChanged(action.filter)
        }
    }

    private fun checkoutTag(tag: Tag) = checkoutCommitUseCase(tag.hash)
    private fun checkoutCommit(commit: Commit) = checkoutCommitUseCase(commit)
    private fun cherryPickCommit(commit: Commit) = cherryPickCommitUseCase(commit)
    private fun revertCommit(commit: Commit) = revertCommitUseCase(commit)

    fun selectUncommittedChanges() = viewModelScope.launch {
        selectedItem.value = SelectedItem.UncommittedChanges

        val searchValue = logSearchFilterResults.value
        if (searchValue is LogSearch.SearchResults) {
            val lastIndexSelected = getLastIndexSelected()

            logSearchFilterResults.value = searchValue.copy(index = lastIndexSelected)
        }
    }

    private fun getLastIndexSelected(): Int {
        val logSearchFilterResultsValue = logSearchFilterResults.value

        return if (logSearchFilterResultsValue is LogSearch.SearchResults) {
            logSearchFilterResultsValue.index
        } else
            NONE_MATCHING_INDEX
    }

    fun selectCommit(commit: Commit) = viewModelScope.launch {
        selectedItem.value = SelectedItem.CommitItem(commit, isStash = false)

        val searchValue = logSearchFilterResults.value
        if (searchValue is LogSearch.SearchResults) {
            var index = searchValue.commits.indexOfFirst { it.hash == commit.hash }

            if (index == -1)
                index = getLastIndexSelected()
            else
                index += 1  // +1 because UI count starts at 1

            logSearchFilterResults.value = searchValue.copy(index = index)
        }
    }

    fun onSearchValueChanged(searchTerm: String) = viewModelScope.launch {
        val logStatusValue = logState.value

        savedSearchFilter = searchTerm

        if (searchTerm.isNotBlank()) {
            val lowercaseValue = searchTerm.lowercase()
            val plotCommitList = logStatusValue.commitList

            val matchingCommits = plotCommitList.commits.filter {
                it.value.message.lowercase().contains(lowercaseValue) ||
                        it.value.author.name.orEmpty().lowercase().contains(lowercaseValue) ||
                        it.value.committer.name.orEmpty().lowercase().contains(lowercaseValue) ||
                        it.value.hash.lowercase().contains(lowercaseValue)
            }

            var startingUiIndex = NONE_MATCHING_INDEX

            if (matchingCommits.isNotEmpty()) {
                _focusCommit.emit(matchingCommits.entries.first().value)
                startingUiIndex = FIRST_INDEX
            }

            // TODO Instead of casting commits to list, use LinkedHashMap everywhere
            logSearchFilterResults.value = LogSearch.SearchResults(matchingCommits.values.toList(), startingUiIndex)
        } else {
            logSearchFilterResults.value = LogSearch.SearchResults(emptyList(), NONE_MATCHING_INDEX)
        }
    }

    suspend fun selectPreviousFilterCommit() {
        val logSearchFilterResultsValue = logSearchFilterResults.value

        if (logSearchFilterResultsValue !is LogSearch.SearchResults) {
            return
        }

        val index = logSearchFilterResultsValue.index
        val commits = logSearchFilterResultsValue.commits

        if (index == NONE_MATCHING_INDEX || index == FIRST_INDEX)
            return

        val newIndex = index - 1
        val newCommitToSelect = commits[newIndex - 1]

        logSearchFilterResults.value = logSearchFilterResultsValue.copy(index = newIndex)
        _focusCommit.emit(newCommitToSelect)
    }

    suspend fun selectNextFilterCommit() {
        val logSearchFilterResultsValue = logSearchFilterResults.value

        if (logSearchFilterResultsValue !is LogSearch.SearchResults) {
            return
        }

        val index = logSearchFilterResultsValue.index
        val commits = logSearchFilterResultsValue.commits
        val totalCount = logSearchFilterResultsValue.totalCount

        if (index == NONE_MATCHING_INDEX || index == totalCount)
            return

        val newIndex = index + 1
        // Use index instead of newIndex because Kotlin arrays start at 0 while the UI count starts at 1
        val newCommitToSelect = commits[index]

        logSearchFilterResults.value = logSearchFilterResultsValue.copy(index = newIndex)
        _focusCommit.emit(newCommitToSelect)
    }

    fun closeSearch() {
        logSearchFilterResults.value = LogSearch.NotSearching
    }

    fun addSearchToCloseableView() = tabScope.launch {
        addCloseableView(CloseableView.LOG_SEARCH)
    }

    private fun removeSearchFromCloseableView() = tabScope.launch {
        removeCloseableView(CloseableView.LOG_SEARCH)
    }

    private fun rebaseInteractive(commit: Commit) = startRebaseInteractiveUseCase(commit)

    fun loadMoreLogItems(firstVisibleItemIndex: Int) = viewModelScope.launch {
        // TODO Refactor this after refactoring
        /*val numberOfCommitsDisplayed = (_logStatus.value as? LogStatus.Loaded)
            ?.plotCommitList
            ?.commits
            .orEmpty()
            .count()

        if (
            loadItemsMutex.isLocked ||
            lastIndexUsedToLoadData in firstVisibleItemIndex..numberOfCommitsDisplayed // TODO what happens if the number of commits has been somehow reduced?
        ) {
            return@runOperation
        }
        loadItemsMutex.withLock {
            lastIndexUsedToLoadData = firstVisibleItemIndex
            val logStatusValue = logStatus.value

            if (logStatusValue !is LogStatus.Loaded)
                return@runOperation

            val currentBranch = getCurrentBranchGitAction(git)

            val statusSummary = getStatusSummaryGitAction(
                git = git,
            )

            val hasUncommittedChanges = statusSummary.total > 0

            val log = getLogGitAction(
                git = git,
                currentBranch = currentBranch,
                hasUncommittedChanges = hasUncommittedChanges,
                commitsLimit = logStatusValue.plotCommitList.commits.count() + INCREMENTAL_COMMITS_LOAD,
                // TODO Reenable lazy loading later: cachedCommitList = logStatusValue.plotCommitList,
            )

            _logStatus.value =
                LogStatus.Loaded(hasUncommittedChanges, log, currentBranch, statusSummary)
        }*/
    }


    private fun alternateShowAsTree() = tabScope.launch {
        appSettings.setConfiguration(AppConfig.ShowChangesAsTree(!appSettings.showChangesAsTree.first()))
    }


    fun selectEntries(entries: List<DiffEntry>) {
        diffSelected.value = addSelectedDiffUseCase(
            diffSelected = diffSelected.value,
            diffType = entries.map { DiffType.CommitDiff(it) },
            addToExisting = false,
        )
    }

    private val refreshDiffFlow = repositoryStateRepository
        .completedTasks
        .map { tasks ->
            tasks.filter { task ->
                task is CompletedTask.Success && (
                        task.taskType is TaskType.StageFile ||
                                task.taskType is TaskType.DoCommit ||
                                task.taskType is TaskType.StageAllFiles ||
                                task.taskType is TaskType.StageHunk ||
                                task.taskType is TaskType.StageLine ||
                                task.taskType is TaskType.StageDir ||
                                task.taskType is TaskType.UnstageAllFiles ||
                                task.taskType is TaskType.UnstageFile ||
                                task.taskType is TaskType.UnstageHunk ||
                                task.taskType is TaskType.UnstageDir ||
                                task.taskType is TaskType.UnstageLine
                        )
            }
        }
        .distinctUntilChanged()

    val diffResult: StateFlow<ViewDiffResult?> = combine(
        diffSelected,
        refreshDiffFlow,
    ) { diffSelected, _ ->
        if (diffSelected?.entries?.count() == 1) {
            val diff = loadDiff(diffSelected.entries.first())

            if (diff is ViewDiffResult.Loaded) {
                addToCloseables()
            }

            diff
        } else {
            ViewDiffResult.DiffNotFound(null)
        }
    }.stateIn(initialValue = null as ViewDiffResult?)

    val diffTypeFlow = settings.diffTextViewType
    val isDisplayFullFile = settings.diffDisplayFullFile

    val isRepositoryInSafeState = repositoryDataRepository.repositoryState
        .map { it == RepositoryState.SAFE }

    private var diffJob: Job? = null

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    private suspend fun loadDiff(diffType: DiffType): ViewDiffResult {
        return getDiffUseCase(diffType)
    }

    fun stageHunk(diffEntry: DiffEntry, hunk: Hunk) = stageHunkUseCase(diffEntry, hunk)

    fun resetHunk(diffEntry: DiffEntry, hunk: Hunk) = resetHunkUseCase(diffEntry, hunk)

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = unstageHunkUseCase(diffEntry, hunk)

    fun stageFile(statusEntry: StatusEntry) = statusStageUseCase(statusEntry)

    fun unstageFile(statusEntry: StatusEntry) = statusUnstageUseCase(statusEntry)

    fun cancelRunningJobs() {
        diffJob?.cancel()
    }

    fun changeTextDiffType(newDiffType: DiffTextViewType) = tabScope.launch {
        settings.setConfiguration(AppConfig.DiffTextViewType(newDiffType))
    }

    fun changeDisplayFullFile(isDisplayFullFile: Boolean) = tabScope.launch {
        settings.setConfiguration(AppConfig.DiffDisplayFullFile(isDisplayFullFile))
    }

    fun stageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = stageHunkLineUseCase(entry, hunk, line)

    fun unstageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = unstageHunkLineUseCase(entry, hunk, line)

    fun openFileWithExternalApp(path: String) {
        openFileInExternalAppUseCase(path)
    }

    fun discardHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = discardHunkLineUseCase(entry, hunk, line)

    fun openSubmodule(path: String) {
        val repositoryPath = repositoryDataRepository.repositoryPath

        // TODO RepositoryPath point to .git dir instead of worktree? Fix if so
        if (repositoryPath != null) {
            viewModelScope.launch {
                appViewModel.addNewTabFromPath("$repositoryPath/$path", true)
            }
        }
    }

    fun addToCloseables() = tabScope.launch {
        addCloseableView(CloseableView.DIFF)
    }

    private fun removeFromCloseables() = tabScope.launch {
        removeCloseableView(CloseableView.DIFF)
    }

    fun reset() {
        cancelRunningJobs()
        removeFromCloseables()
    }

    fun clearDiff() {
        val diff = when (val state = diffResult.value) {
            is ViewDiffResult.DiffNotFound -> state.diffType
            is ViewDiffResult.Loaded -> state.diffType
            is ViewDiffResult.Loading -> state.diffType
            else -> null
        }

        if (diff != null) {
            when (diff) {
                is DiffType.CommitDiff -> removeSelectedDiff(setOf(diff))
                is DiffType.UncommittedDiff -> removeSelectedDiff(
                    setOf(diff),
                    diff.entryType,
                )
            }
        }
    }

    val rebaseState: StateFlow<RebaseInteractiveViewState>
        field = MutableStateFlow<RebaseInteractiveViewState>(RebaseInteractiveViewState.Loading)

    var rewordSteps = ArrayDeque<RebaseLine>()

    private var interactiveHandlerContinue = object : InteractiveHandler {
        override fun prepareSteps(steps: MutableList<RebaseTodoLine>) {
            val rebaseState = rebaseState.value
            if (rebaseState !is RebaseInteractiveViewState.Loaded) {
                throw Exception("prepareSteps called when rebaseState is not Loaded") // Should never happen, just in case
            }

            val newSteps = rebaseState.stepsList.toMutableList()
            rewordSteps = ArrayDeque(newSteps.filter { it.rebaseAction == RebaseAction.REWORD })

            val newRebaseTodoLines = newSteps
                .filter { it.rebaseAction != RebaseAction.DROP } // Remove dropped lines
                .map { it.toRebaseTodoLine() }

            steps.clear()
            steps.addAll(newRebaseTodoLines)
        }

        override fun modifyCommitMessage(commit: String): String {
            // This can be called when there aren't any reword steps if squash is used.
            val step = rewordSteps.removeFirstOrNull() ?: return commit

            val rebaseState = rebaseState.value
            if (rebaseState !is RebaseInteractiveViewState.Loaded) {
                throw Exception("modifyCommitMessage called when rebaseState is not Loaded") // Should never happen, just in case
            }

            return rebaseState.messages[step.commit.name()]
                ?: throw InvalidMessageException("Message for commit $commit is unexpectedly null")
        }
    }

    fun removeSelectedDiff(selectedToRemove: Set<DiffType.CommitDiff>) {
        val diffSelected = diffSelected.value

        if (diffSelected is DiffSelected.CommitedChanges) {
            val newDiffSelected = diffSelected.copy(items = diffSelected.items - selectedToRemove)
            this.diffSelected.value = newDiffSelected
        }
    }

    fun removeSelectedDiff(selectedToRemove: Set<DiffType.UncommittedDiff>, entryType: EntryType) {
        val diffSelected = diffSelected.value

        if (diffSelected is DiffSelected.UncommittedChanges && diffSelected.entryType == entryType) {
            val newDiffSelected = diffSelected.copy(items = diffSelected.items - selectedToRemove)
            this.diffSelected.value = newDiffSelected
        }
    }

    fun loadRebaseInteractiveData() {
        // TODO Move this to use case
//        viewModelScope.launch {
//            val stateResult = getRepositoryStateUseCase()
//
//            when (stateResult) {
//                is Either.Err -> {
//                    _rebaseState.value = RebaseInteractiveViewState.Failed(stateResult.error.toString())
//                }
//
//                is Either.Ok -> {
//                    val state = stateResult.value
//                    if (!state.isRebasing) {
//                        _rebaseState.value = RebaseInteractiveViewState.Loading
//                        return@launch
//                    }
//                }
//            }
//        }
//
//
//
//        try {
//            val lines = getRebaseInteractiveTodoLinesGitAction(git)
//            val messages = getRebaseLinesFullMessageGitAction(tabState.git, lines)
//            val rebaseLines = lines.map {
//                RebaseLine(
//                    it.action.toRebaseAction(),
//                    it.commit,
//                    it.shortMessage,
//                )
//            }
//
//            val isSameRebase = isSameRebase(rebaseLines, _rebaseState.value)
//
//            if (!isSameRebase) {
//                _rebaseState.value = RebaseInteractiveViewState.Loaded(rebaseLines, messages)
//                val firstLine = rebaseLines.firstOrNull()
//
//                if (firstLine != null) {
//                    val fullCommit = getCommitFromRebaseLineUseCase(firstLine.commit, firstLine.shortMessage)
//                    tabState.newSelectedCommit(fullCommit)
//                }
//            }
//
//        } catch (ex: Exception) {
//            if (ex is RebaseCancelledException) {
//                println("Rebase cancelled")
//            } else {
//                cancel()
//                throw ex
//            }
//        }
//
//        null
    }

    private fun isSameRebase(rebaseLines: List<RebaseLine>, state: RebaseInteractiveViewState): Boolean {
        if (state is RebaseInteractiveViewState.Loaded) {
            val stepsList = state.stepsList

            if (rebaseLines.count() != stepsList.count()) {
                return false
            }

            return rebaseLines.map { it.commit.name() } == stepsList.map { it.commit.name() }
        }

        return false
    }

    fun continueRebaseInteractive() = resumeRebaseInteractiveUseCase(interactiveHandlerContinue)

    fun onCommitMessageChanged(commit: AbbreviatedObjectId, message: String) {
        val rebaseState = rebaseState.value

        if (rebaseState !is RebaseInteractiveViewState.Loaded)
            return

        val messagesMap = rebaseState.messages.toMutableMap()
        messagesMap[commit.name()] = message

        this.rebaseState.value = rebaseState.copy(messages = messagesMap)
    }

    fun onCommitActionChanged(commit: AbbreviatedObjectId, rebaseAction: RebaseAction) {
        val rebaseState = rebaseState.value

        if (rebaseState !is RebaseInteractiveViewState.Loaded)
            return

        val newStepsList =
            rebaseState.stepsList.toMutableList() // Change the list reference to update the flow with .toList()

        val stepIndex = newStepsList.indexOfFirst {
            it.commit == commit
        }

        if (stepIndex >= 0) {
            val step = newStepsList[stepIndex]
            val newTodoLine = RebaseLine(
                rebaseAction,
                step.commit,
                step.shortMessage
            )

            newStepsList[stepIndex] = newTodoLine

            this.rebaseState.value = rebaseState.copy(stepsList = newStepsList)
        }
    }

    fun cancel() {
        abortRebaseUseCase()
        rebaseState.value = RebaseInteractiveViewState.Loading
    }

    fun selectLine(line: RebaseLine) = viewModelScope.launch {
        val fullCommit = getCommitFromRebaseLineUseCase(TODO()/*line.commit*/, line.shortMessage)
//        tabState.newSelectedCommit(TODO()/*line.commit*/)

        null
    }

    fun moveCommit(from: Int, to: Int) {
        val state = rebaseState.value

        if (state is RebaseInteractiveViewState.Loaded) {

            val newStepsList = state.stepsList.toMutableList().apply {
                add(to, removeAt(from))
            }

            this.rebaseState.value = state.copy(stepsList = newStepsList)
        }
    }

    fun openFileInFolder(path: String) {
        TODO()
    }
}


sealed interface BlameState {
    data class Loading(val filePath: String) : BlameState

    data class Loaded(val filePath: String, val blameResult: BlameResult, val isMinimized: Boolean = false) : BlameState

    data object None : BlameState
}