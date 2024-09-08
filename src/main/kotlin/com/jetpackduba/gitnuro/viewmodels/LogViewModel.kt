package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.extensions.shortName
import com.jetpackduba.gitnuro.git.CloseableView
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.TaskEvent
import com.jetpackduba.gitnuro.git.branches.CreateBranchOnCommitUseCase
import com.jetpackduba.gitnuro.git.branches.GetCurrentBranchUseCase
import com.jetpackduba.gitnuro.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.git.graph.GraphNode
import com.jetpackduba.gitnuro.git.log.*
import com.jetpackduba.gitnuro.git.rebase.StartRebaseInteractiveUseCase
import com.jetpackduba.gitnuro.git.tags.CreateTagOnCommitUseCase
import com.jetpackduba.gitnuro.git.workspace.CheckHasUncommittedChangesUseCase
import com.jetpackduba.gitnuro.git.workspace.GetStatusSummaryUseCase
import com.jetpackduba.gitnuro.git.workspace.StatusSummary
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.log.LogDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

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

class LogViewModel @Inject constructor(
    private val getLogUseCase: GetLogUseCase,
    private val getStatusSummaryUseCase: GetStatusSummaryUseCase,
    private val checkHasUncommittedChangesUseCase: CheckHasUncommittedChangesUseCase,
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
    private val createBranchOnCommitUseCase: CreateBranchOnCommitUseCase,
    private val checkoutCommitUseCase: CheckoutCommitUseCase,
    private val revertCommitUseCase: RevertCommitUseCase,
    private val resetToCommitUseCase: ResetToCommitUseCase,
    private val cherryPickCommitUseCase: CherryPickCommitUseCase,
    private val createTagOnCommitUseCase: CreateTagOnCommitUseCase,
    private val startRebaseInteractiveUseCase: StartRebaseInteractiveUseCase,
    private val tabState: TabState,
    private val appSettingsRepository: AppSettingsRepository,
    private val tabScope: CoroutineScope,
    sharedStashViewModel: SharedStashViewModel,
    sharedBranchesViewModel: SharedBranchesViewModel,
    sharedRemotesViewModel: SharedRemotesViewModel,
    sharedTagsViewModel: SharedTagsViewModel,
) : ViewModel,
    ISharedStashViewModel by sharedStashViewModel,
    ISharedBranchesViewModel by sharedBranchesViewModel,
    ISharedRemotesViewModel by sharedRemotesViewModel,
    ISharedTagsViewModel by sharedTagsViewModel {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loading)

    val logStatus: StateFlow<LogStatus>
        get() = _logStatus

    var savedSearchFilter: String = ""
    var graphPadding = 0f

    private val scrollToItem: Flow<RevCommit> = tabState.taskEvent
        .filterIsInstance<TaskEvent.ScrollToGraphItem>()
        .map { it.selectedItem }
        .filterIsInstance<SelectedItem.CommitBasedItem>()
        .map { it.revCommit }

    val scrollToUncommittedChanges: Flow<SelectedItem.UncommittedChanges> = tabState.taskEvent
        .filterIsInstance<TaskEvent.ScrollToGraphItem>()
        .map { it.selectedItem }
        .filterIsInstance()

    private val _focusCommit = MutableSharedFlow<RevCommit>()
    val focusCommit: Flow<RevCommit> = merge(_focusCommit, scrollToItem)

    private val _logDialog = MutableStateFlow<LogDialog>(LogDialog.None)
    val logDialog: StateFlow<LogDialog> = _logDialog

    val verticalListState = MutableStateFlow(LazyListState(0, 0))
    val horizontalListState = MutableStateFlow(ScrollState(0))

    private val _logSearchFilterResults = MutableStateFlow<LogSearch>(LogSearch.NotSearching)
    val logSearchFilterResults: StateFlow<LogSearch> = _logSearchFilterResults

    init {
        tabScope.launch {
            appSettingsRepository.commitsLimitEnabledFlow.drop(1).collectLatest {
                tabState.refreshData(RefreshType.ONLY_LOG)
            }
        }
        tabScope.launch {
            appSettingsRepository.commitsLimitFlow.collectLatest {
                tabState.refreshData(RefreshType.ONLY_LOG)
            }
        }

        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.ALL_DATA,
                RefreshType.ONLY_LOG,
                RefreshType.UNCOMMITTED_CHANGES,
                RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
            ) { refreshType ->
                if (refreshType == RefreshType.UNCOMMITTED_CHANGES) {
                    uncommittedChangesLoadLog(tabState.git)
                } else
                    refresh(tabState.git)
            }
        }

        tabScope.launch {
            tabState.closeViewFlow.collectLatest {
                if (it == CloseableView.LOG_SEARCH) {
                    _logSearchFilterResults.value = LogSearch.NotSearching
                }
            }
        }

        tabScope.launch {
            _logSearchFilterResults.collectLatest {
                when (it) {
                    LogSearch.NotSearching -> removeSearchFromCloseableView()
                    is LogSearch.SearchResults -> addSearchToCloseableView()
                }
            }
        }
    }


    private suspend fun loadLog(git: Git) = delayedStateChange(
        delayMs = LOG_MIN_TIME_IN_MS_TO_SHOW_LOAD,
        onDelayTriggered = {
            _logStatus.value = LogStatus.Loading
        }
    ) {
        val currentBranch = getCurrentBranchUseCase(git)

        val statusSummary = getStatusSummaryUseCase(
            git = git,
        )

        val hasUncommittedChanges = statusSummary.total > 0
        val commitsLimit = if (appSettingsRepository.commitsLimitEnabled) {
            appSettingsRepository.commitsLimit
        } else
            Int.MAX_VALUE

        val commitsLimitDisplayed = if (appSettingsRepository.commitsLimitEnabled) {
            appSettingsRepository.commitsLimit
        } else
            -1

        val log = getLogUseCase(git, currentBranch, hasUncommittedChanges, commitsLimit)

        _logStatus.value =
            LogStatus.Loaded(hasUncommittedChanges, log, currentBranch, statusSummary, commitsLimitDisplayed)

        // Remove search filter if the log has been updated
        _logSearchFilterResults.value = LogSearch.NotSearching
    }

    fun checkoutCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Commit checkout",
        subtitle = "Checking out commit ${revCommit.name}",
        taskType = TaskType.CHECKOUT_COMMIT,
    ) { git ->
        checkoutCommitUseCase(git, revCommit)

        positiveNotification("Commit checked out")
    }

    fun revertCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Commit revert",
        subtitle = "Reverting commit ${revCommit.name}",
        refreshEvenIfCrashes = true,
        taskType = TaskType.REVERT_COMMIT,
    ) { git ->
        revertCommitUseCase(git, revCommit)

        positiveNotification("Commit reverted")
    }

    fun resetToCommit(revCommit: RevCommit, resetType: ResetType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch reset",
        subtitle = "Resetting branch to commit ${revCommit.shortName}",
        taskType = TaskType.RESET_TO_COMMIT,
    ) { git ->
        resetToCommitUseCase(git, revCommit, resetType = resetType)

        positiveNotification("Reset completed")
    }

    fun cherryPickCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        title = "Cherry-pick",
        subtitle = "Cherry-picking commit ${revCommit.shortName}",
        taskType = TaskType.CHERRY_PICK_COMMIT,
        refreshEvenIfCrashes = true,
    ) { git ->
        cherryPickCommitUseCase(git, revCommit)

        positiveNotification("Commit cherry-picked")
    }

    fun createBranchOnCommit(branch: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "New branch",
        subtitle = "Creating new branch \"$branch\" on commit ${revCommit.shortName}",
        refreshEvenIfCrashesInteractive = { it is CheckoutConflictException },
        taskType = TaskType.CREATE_BRANCH,
    ) { git ->
        createBranchOnCommitUseCase(git, branch, revCommit)

        positiveNotification("Branch \"$branch\" created")
    }

    fun createTagOnCommit(tag: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "New tag",
        subtitle = "Creating new tag \"$tag\" on commit ${revCommit.shortName}",
        taskType = TaskType.CREATE_TAG,
    ) { git ->
        createTagOnCommitUseCase(git, tag, revCommit)

        positiveNotification("Tag created")
    }

    private suspend fun uncommittedChangesLoadLog(git: Git) {
        val currentBranch = getCurrentBranchUseCase(git)
        val hasUncommittedChanges = checkHasUncommittedChangesUseCase(git)

        val statsSummary = if (hasUncommittedChanges) {
            getStatusSummaryUseCase(
                git = git,
            )
        } else
            StatusSummary(0, 0, 0, 0)

        val previousLogStatusValue = _logStatus.value

        if (previousLogStatusValue is LogStatus.Loaded) {
            val newLogStatusValue = LogStatus.Loaded(
                hasUncommittedChanges = hasUncommittedChanges,
                plotCommitList = previousLogStatusValue.plotCommitList,
                currentBranch = currentBranch,
                statusSummary = statsSummary,
                commitsLimit = previousLogStatusValue.commitsLimit,
            )

            _logStatus.value = newLogStatusValue
        }
    }

    suspend fun refresh(git: Git) {
        loadLog(git)
    }

    fun selectUncommittedChanges() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        tabState.newSelectedItem(SelectedItem.UncommittedChanges)

        val searchValue = _logSearchFilterResults.value
        if (searchValue is LogSearch.SearchResults) {
            val lastIndexSelected = getLastIndexSelected()

            _logSearchFilterResults.value = searchValue.copy(index = lastIndexSelected)
        }
    }

    private fun getLastIndexSelected(): Int {
        val logSearchFilterResultsValue = logSearchFilterResults.value

        return if (logSearchFilterResultsValue is LogSearch.SearchResults) {
            logSearchFilterResultsValue.index
        } else
            NONE_MATCHING_INDEX
    }

    fun selectCommit(commit: GraphNode) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        tabState.newSelectedCommit(commit)

        val searchValue = _logSearchFilterResults.value
        if (searchValue is LogSearch.SearchResults) {
            var index = searchValue.commits.indexOf(commit)

            if (index == -1)
                index = getLastIndexSelected()
            else
                index += 1  // +1 because UI count starts at 1

            _logSearchFilterResults.value = searchValue.copy(index = index)
        }
    }

    suspend fun onSearchValueChanged(searchTerm: String) {
        val logStatusValue = logStatus.value

        if (logStatusValue !is LogStatus.Loaded)
            return

        savedSearchFilter = searchTerm

        if (searchTerm.isNotBlank()) {
            val lowercaseValue = searchTerm.lowercase()
            val plotCommitList = logStatusValue.plotCommitList

            val matchingCommits = plotCommitList.filter {
                it.fullMessage.lowercase().contains(lowercaseValue) ||
                        it.authorIdent.name.lowercase().contains(lowercaseValue) ||
                        it.committerIdent.name.lowercase().contains(lowercaseValue) ||
                        it.name.lowercase().contains(lowercaseValue)
            }

            var startingUiIndex = NONE_MATCHING_INDEX

            if (matchingCommits.isNotEmpty()) {
                _focusCommit.emit(matchingCommits.first())
                startingUiIndex = FIRST_INDEX
            }

            _logSearchFilterResults.value = LogSearch.SearchResults(matchingCommits, startingUiIndex)
        } else {
            _logSearchFilterResults.value = LogSearch.SearchResults(emptyList(), NONE_MATCHING_INDEX)
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

        _logSearchFilterResults.value = logSearchFilterResultsValue.copy(index = newIndex)
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

        _logSearchFilterResults.value = logSearchFilterResultsValue.copy(index = newIndex)
        _focusCommit.emit(newCommitToSelect)
    }

    fun showDialog(dialog: LogDialog) {
        _logDialog.value = dialog
    }

    fun closeSearch() {
        _logSearchFilterResults.value = LogSearch.NotSearching
    }

    fun addSearchToCloseableView() = tabScope.launch {
        tabState.addCloseableView(CloseableView.LOG_SEARCH)
    }

    private fun removeSearchFromCloseableView() = tabScope.launch {
        tabState.removeCloseableView(CloseableView.LOG_SEARCH)
    }

    fun rebaseInteractive(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.REBASE_INTERACTIVE_STATE,
        taskType = TaskType.REBASE_INTERACTIVE,
    ) { git ->
        startRebaseInteractiveUseCase(git, revCommit)

        null
    }
}

sealed interface LogStatus {
    data object Loading : LogStatus
    class Loaded(
        val hasUncommittedChanges: Boolean,
        val plotCommitList: GraphCommitList,
        val currentBranch: Ref?,
        val statusSummary: StatusSummary,
        val commitsLimit: Int,
    ) : LogStatus
}

sealed interface LogSearch {
    data object NotSearching : LogSearch
    data class SearchResults(
        val commits: List<GraphNode>,
        val index: Int,
        val totalCount: Int = commits.count(),
    ) : LogSearch
}
