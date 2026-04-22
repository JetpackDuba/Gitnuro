package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.countOrZero
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ICherryPickCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IRevertCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IStartRebaseInteractiveGitAction
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.StartRebaseInteractiveUseCase
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.jetbrains.skiko.ClipboardManager
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

private const val INITIAL_COMMITS_LOAD = 2000
const val INCREMENTAL_COMMITS_LOAD = 1000

class LogViewModel @Inject constructor(
    private val checkoutCommitGitAction: ICheckoutCommitGitAction,
    private val revertCommitGitAction: IRevertCommitGitAction,
    private val cherryPickCommitGitAction: ICherryPickCommitGitAction,
    private val startRebaseInteractiveGitAction: IStartRebaseInteractiveGitAction,
    private val tabState: TabInstanceRepository,
    private val tabScope: TabCoroutineScope,
    private val clipboardManager: ClipboardManager,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val startRebaseInteractiveUseCase: StartRebaseInteractiveUseCase,
    sharedStashViewModel: SharedStashViewModel,
    sharedBranchesViewModel: SharedBranchesViewModel,
    sharedRemotesViewModel: SharedRemotesViewModel,
    sharedTagsViewModel: SharedTagsViewModel,
) : ISharedStashViewModel by sharedStashViewModel,
    ISharedBranchesViewModel by sharedBranchesViewModel,
    ISharedRemotesViewModel by sharedRemotesViewModel,
    ISharedTagsViewModel by sharedTagsViewModel,
    TabViewModel() {

    private val hasUncommittedChanges = repositoryDataRepository.status.map {
        it.staged.isNotEmpty() || it.unstaged.isNotEmpty()
    }

    private val log = repositoryDataRepository.log
    private val currentBranch = repositoryDataRepository.currentBranch
    private val statusSummary = repositoryDataRepository.status.map {
        getSummary(it)
    }


    private val verticalListState = MutableStateFlow(LazyListState(0, 0))
    private val horizontalListState = MutableStateFlow(ScrollState(0))


    private val _logSearchFilterResults = MutableStateFlow<LogSearch>(LogSearch.NotSearching)
    val logSearchFilterResults: StateFlow<LogSearch> = _logSearchFilterResults

    val logState = combine(
        log,
        hasUncommittedChanges,
        currentBranch,
        statusSummary,
        logSearchFilterResults,
        verticalListState,
        horizontalListState,
    ) { log,
        hasUncommittedChanges,
        currentBranch,
        statusSummary,
        logSearchFilterResults,
        verticalListState,
        horizontalListState ->
        LogState(
            isLoading = false,
            hasUncommittedChanges,
            log,
            currentBranch,
            statusSummary,
            logSearchFilterResults,
            verticalListState,
            horizontalListState,
        )
    }
        .debounce(LOG_MIN_TIME_IN_MS_TO_SHOW_LOAD)
        .stateIn(
            scope = tabScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = LogState(true),
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

    init {
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

    fun checkoutCommit(revCommit: Commit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Commit checkout",
        subtitle = "Checking out commit ${revCommit.hash}",
        taskType = TaskType.CHECKOUT_COMMIT,
    ) { git ->
        checkoutCommitGitAction(git, revCommit)

        positiveNotification("Commit checked out")
    }

    fun revertCommit(revCommit: Commit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Commit revert",
        subtitle = "Reverting commit ${revCommit.hash}",
        refreshEvenIfCrashes = true,
        taskType = TaskType.REVERT_COMMIT,
    ) { git ->
        revertCommitGitAction(git, revCommit)

        positiveNotification("Commit reverted")
    }

    fun cherryPickCommit(revCommit: Commit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        title = "Cherry-pick",
        subtitle = "Cherry-picking commit ${revCommit.shortHash}",
        taskType = TaskType.CHERRY_PICK_COMMIT,
        refreshEvenIfCrashes = true,
    ) { git ->
        cherryPickCommitGitAction(git, revCommit)

        positiveNotification("Commit cherry-picked")
    }

    fun selectUncommittedChanges() = viewModelScope.launch {
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

    fun selectCommit(commit: Commit) = viewModelScope.launch {
        tabState.newSelectedCommit(commit)

        val searchValue = _logSearchFilterResults.value
        if (searchValue is LogSearch.SearchResults) {
            var index = searchValue.commits.indexOfFirst { it.hash == commit.hash }

            if (index == -1)
                index = getLastIndexSelected()
            else
                index += 1  // +1 because UI count starts at 1

            _logSearchFilterResults.value = searchValue.copy(index = index)
        }
    }

    fun onSearchValueChanged(searchTerm: String) = viewModelScope.launch {
        val logStatusValue = logState.value

        savedSearchFilter = searchTerm

        if (searchTerm.isNotBlank()) {
            val lowercaseValue = searchTerm.lowercase()
            val plotCommitList = logStatusValue.commitList

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

    private fun rebaseInteractive(commit: Commit) = startRebaseInteractiveUseCase(commit)

    fun loadMoreLogItems(firstVisibleItemIndex: Int) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
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

    override fun copyBranchNameToClipboard(branch: Branch) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
        taskType = TaskType.UNSPECIFIED
    ) {
        copyBranchNameToClipboardAndGetNotification(
            branch,
            clipboardManager
        )
    }

    fun getSummary(status: Status): StatusSummary {
        val staged = status.staged

        val unstaged = status.unstaged
        val allChanges = staged + unstaged

        val groupedChanges = allChanges.groupBy {
            it.statusType
        }

        val deletedCount = groupedChanges[StatusType.REMOVED].countOrZero()
        val addedCount = groupedChanges[StatusType.ADDED].countOrZero()

        val modifiedCount = groupedChanges[StatusType.MODIFIED].countOrZero()
        val conflictingCount = groupedChanges[StatusType.CONFLICTING].countOrZero()

        return StatusSummary(
            modifiedCount = modifiedCount,
            deletedCount = deletedCount,
            addedCount = addedCount,
            conflictingCount = conflictingCount,
        )
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
            is LogAction.CopyBranchNameToClipboard -> copyBranchNameToClipboard(action.branch)
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
}

@Stable
data class LogState(
    val isLoading: Boolean,
    val hasUncommittedChanges: Boolean = false,
    val commitList: GraphCommits = GraphCommits(emptyList(), 0),
    val currentBranch: Branch? = null,
    val statusSummary: StatusSummary = StatusSummary(0, 0, 0, 0),
    val searchFilter: LogSearch = LogSearch.NotSearching,
    val verticalScrollState: LazyListState = LazyListState(),
    val horizontalScrollState: ScrollState = ScrollState(0),
)

sealed interface LogSearch {
    data object NotSearching : LogSearch
    data class SearchResults(
        val commits: List<GraphCommit>,
        val index: Int,
        val totalCount: Int = commits.count(),
    ) : LogSearch
}


sealed interface LogAction {
    data class Merge(val branch: Branch) : LogAction
    data class Rebase(val branch: Branch) : LogAction
    data class DeleteBranch(val branch: Branch) : LogAction
    data class CheckoutCommit(val commit: Commit) : LogAction
    data class RevertCommit(val commit: Commit) : LogAction
    data class CherryPickCommit(val commit: Commit) : LogAction
    data class CheckoutRemoteBranch(val branch: Branch) : LogAction
    data class CheckoutBranch(val branch: Branch) : LogAction
    data class RebaseInteractive(val commit: Commit) : LogAction
    data class CommitSelected(val commit: Commit) : LogAction
    data object UncommittedChangesSelected : LogAction
    data class DeleteStash(val commit: Commit) : LogAction
    data class ApplyStash(val commit: Commit) : LogAction
    data class PopStash(val commit: Commit) : LogAction
    data class CheckoutTag(val tag: Tag) : LogAction
    data class DeleteRemoteBranch(val branch: Branch) : LogAction
    data class DeleteTag(val tag: Tag) : LogAction
    data class PushToRemoteBranch(val branch: Branch) : LogAction
    data class PullFromRemoteBranch(val branch: Branch) : LogAction
    data class CopyBranchNameToClipboard(val branch: Branch) : LogAction
    data class SearchValueChange(val filter: String) : LogAction
}