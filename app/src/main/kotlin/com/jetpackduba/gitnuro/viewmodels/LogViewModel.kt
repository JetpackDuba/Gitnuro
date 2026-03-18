package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.domain.extensions.shortName
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.domain.git.graph.GraphNode
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ICherryPickCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetLogGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IRevertCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IStartRebaseInteractiveGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ICheckHasUncommittedChangesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetStatusSummaryGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import com.jetpackduba.gitnuro.domain.models.StatusSummary
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Inject
import kotlin.math.max

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

private const val INITIAL_COMMITS_LOAD = 2000
const val INCREMENTAL_COMMITS_LOAD = 1000

class LogViewModel @Inject constructor(
    private val getLogGitAction: IGetLogGitAction,
    private val getStatusSummaryGitAction: IGetStatusSummaryGitAction,
    private val checkHasUncommittedChangesGitAction: ICheckHasUncommittedChangesGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val checkoutCommitGitAction: ICheckoutCommitGitAction,
    private val revertCommitGitAction: IRevertCommitGitAction,
    private val cherryPickCommitGitAction: ICherryPickCommitGitAction,
    private val startRebaseInteractiveGitAction: IStartRebaseInteractiveGitAction,
    private val tabState: TabInstanceRepository,
    private val tabScope: CoroutineScope,
    private val clipboardManager: ClipboardManager,
    sharedStashViewModel: SharedStashViewModel,
    sharedBranchesViewModel: SharedBranchesViewModel,
    sharedRemotesViewModel: SharedRemotesViewModel,
    sharedTagsViewModel: SharedTagsViewModel,
) : ISharedStashViewModel by sharedStashViewModel,
    ISharedBranchesViewModel by sharedBranchesViewModel,
    ISharedRemotesViewModel by sharedRemotesViewModel,
    ISharedTagsViewModel by sharedTagsViewModel {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loading)

    val logStatus = _logStatus
        .debounce(LOG_MIN_TIME_IN_MS_TO_SHOW_LOAD)
        .stateIn(
            scope = tabScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _logStatus.value,
        )


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

    val verticalListState = MutableStateFlow(LazyListState(0, 0))
    val horizontalListState = MutableStateFlow(ScrollState(0))

    private val _logSearchFilterResults = MutableStateFlow<LogSearch>(LogSearch.NotSearching)
    val logSearchFilterResults: StateFlow<LogSearch> = _logSearchFilterResults

    init {
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

    private suspend fun loadLog(git: Git) {
        val currentBranch = getCurrentBranchGitAction(git)

        val statusSummary = getStatusSummaryGitAction(
            git = git,
        )

        val hasUncommittedChanges = statusSummary.total > 0

        val log = getLogGitAction(
            git = git,
            currentBranch = currentBranch,
            hasUncommittedChanges = hasUncommittedChanges,
            commitsLimit = max(INITIAL_COMMITS_LOAD, (lastIndexUsedToLoadData + INCREMENTAL_COMMITS_LOAD))
        )

        _logStatus.value =
            LogStatus.Loaded(hasUncommittedChanges, log, currentBranch, statusSummary)

        // Remove search filter if the log has been updated
        _logSearchFilterResults.value = LogSearch.NotSearching
    }

    fun checkoutCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Commit checkout",
        subtitle = "Checking out commit ${revCommit.name}",
        taskType = TaskType.CHECKOUT_COMMIT,
    ) { git ->
        checkoutCommitGitAction(git, revCommit)

        positiveNotification("Commit checked out")
    }

    fun revertCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Commit revert",
        subtitle = "Reverting commit ${revCommit.name}",
        refreshEvenIfCrashes = true,
        taskType = TaskType.REVERT_COMMIT,
    ) { git ->
        revertCommitGitAction(git, revCommit)

        positiveNotification("Commit reverted")
    }

    fun cherryPickCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        title = "Cherry-pick",
        subtitle = "Cherry-picking commit ${revCommit.shortName}",
        taskType = TaskType.CHERRY_PICK_COMMIT,
        refreshEvenIfCrashes = true,
    ) { git ->
        cherryPickCommitGitAction(git, revCommit)

        positiveNotification("Commit cherry-picked")
    }

    private suspend fun uncommittedChangesLoadLog(git: Git) {
        val currentBranch = getCurrentBranchGitAction(git)
        val hasUncommittedChanges = checkHasUncommittedChangesGitAction(git)

        val statsSummary = if (hasUncommittedChanges) {
            getStatusSummaryGitAction(
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

    fun selectCommit(commit: GraphCommit) = tabState.runOperation(
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

            val matchingCommits = plotCommitList.commits.filter {
                it.message.lowercase().contains(lowercaseValue) ||
                        it.author.name.lowercase().contains(lowercaseValue) ||
                        it.committer.name.lowercase().contains(lowercaseValue) ||
                        it.hash.lowercase().contains(lowercaseValue)
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
        startRebaseInteractiveGitAction(git, revCommit)

        null
    }

    fun loadMoreLogItems(firstVisibleItemIndex: Int) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val numberOfCommitsDisplayed = (_logStatus.value as? LogStatus.Loaded)
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
        }
    }

    override fun copyBranchNameToClipboard(branch: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
        taskType = TaskType.UNSPECIFIED
    ) {
        copyBranchNameToClipboardAndGetNotification(
            branch,
            clipboardManager
        )
    }
}

sealed interface LogStatus {
    data object Loading : LogStatus
    class Loaded(
        val hasUncommittedChanges: Boolean,
        val plotCommitList: GraphCommits,
        val currentBranch: Branch?,
        val statusSummary: StatusSummary,
    ) : LogStatus
}

sealed interface LogSearch {
    data object NotSearching : LogSearch
    data class SearchResults(
        val commits: List<GraphCommit>,
        val index: Int,
        val totalCount: Int = commits.count(),
    ) : LogSearch
}
