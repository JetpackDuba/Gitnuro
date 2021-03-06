package app.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import app.preferences.AppPreferences
import app.extensions.delayedStateChange
import app.git.*
import app.git.graph.GraphCommitList
import app.git.graph.GraphNode
import app.ui.SelectedItem
import app.ui.log.LogDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
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
    private val logManager: LogManager,
    private val statusManager: StatusManager,
    private val branchesManager: BranchesManager,
    private val rebaseManager: RebaseManager,
    private val tagsManager: TagsManager,
    private val mergeManager: MergeManager,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val tabState: TabState,
    private val appPreferences: AppPreferences,
) {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loading)

    val logStatus: StateFlow<LogStatus>
        get() = _logStatus

    var savedSearchFilter: String = ""

    private val _focusCommit = MutableSharedFlow<GraphNode>()
    val focusCommit: SharedFlow<GraphNode> = _focusCommit

    private val _logDialog = MutableStateFlow<LogDialog>(LogDialog.None)
    val logDialog: StateFlow<LogDialog> = _logDialog

    val verticalListState = MutableStateFlow(LazyListState(0, 0))
    val horizontalListState = MutableStateFlow(ScrollState(0))

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _logSearchFilterResults = MutableStateFlow<LogSearch>(LogSearch.NotSearching)
    val logSearchFilterResults: StateFlow<LogSearch> = _logSearchFilterResults

    init {
        scope.launch {
            appPreferences.commitsLimitEnabledFlow.collect {
                tabState.refreshData(RefreshType.ONLY_LOG)
            }
        }
        scope.launch {
            appPreferences.commitsLimitFlow.collect {
                tabState.refreshData(RefreshType.ONLY_LOG)
            }
        }
    }

    private suspend fun loadLog(git: Git) = delayedStateChange(
        delayMs = LOG_MIN_TIME_IN_MS_TO_SHOW_LOAD,
        onDelayTriggered = {
            _logStatus.value = LogStatus.Loading
        }
    ) {
        val currentBranch = branchesManager.currentBranchRef(git)

        val statusSummary = statusManager.getStatusSummary(
            git = git,
        )

        val hasUncommitedChanges = statusSummary.total > 0
        val commitsLimit = if (appPreferences.commitsLimitEnabled) {
            appPreferences.commitsLimit
        } else
            Int.MAX_VALUE

        val commitsLimitDisplayed = if (appPreferences.commitsLimitEnabled) {
            appPreferences.commitsLimit
        } else
            -1

        val log = logManager.loadLog(git, currentBranch, hasUncommitedChanges, commitsLimit)

        _logStatus.value =
            LogStatus.Loaded(hasUncommitedChanges, log, currentBranch, statusSummary, commitsLimitDisplayed)

        // Remove search filter if the log has been updated
        _logSearchFilterResults.value = LogSearch.NotSearching
    }


    fun pushToRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        remoteOperationsManager.pushToBranch(
            git = git,
            force = false,
            pushTags = false,
            remoteBranch = branch,
        )
    }

    fun pullFromRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        remoteOperationsManager.pullFromBranch(
            git = git,
            rebase = false,
            remoteBranch = branch,
        )
    }

    fun checkoutCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        logManager.checkoutCommit(git, revCommit)
    }

    fun revertCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        logManager.revertCommit(git, revCommit)
    }

    fun resetToCommit(revCommit: RevCommit, resetType: ResetType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        logManager.resetToCommit(git, revCommit, resetType = resetType)
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.checkoutRef(git, ref)
    }

    fun cherrypickCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        mergeManager.cherryPickCommit(git, revCommit)
    }

    fun createBranchOnCommit(branch: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.createBranchOnCommit(git, branch, revCommit)
    }

    fun createTagOnCommit(tag: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        tagsManager.createTagOnCommit(git, tag, revCommit)
    }

    fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        mergeManager.mergeBranch(git, ref, appPreferences.ffMerge)
    }

    fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.deleteBranch(git, branch)
    }

    fun deleteTag(tag: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        tagsManager.deleteTag(git, tag)
    }

    suspend fun refreshUncommitedChanges(git: Git) {
        uncommitedChangesLoadLog(git)
    }

    private suspend fun uncommitedChangesLoadLog(git: Git) {
        val currentBranch = branchesManager.currentBranchRef(git)
        val hasUncommitedChanges = statusManager.hasUncommitedChanges(git)

        val statsSummary = if (hasUncommitedChanges) {
            statusManager.getStatusSummary(
                git = git,
            )
        } else
            StatusSummary(0, 0, 0, 0)

        val previousLogStatusValue = _logStatus.value

        if (previousLogStatusValue is LogStatus.Loaded) {
            val newLogStatusValue = LogStatus.Loaded(
                hasUncommitedChanges = hasUncommitedChanges,
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

    fun rebaseBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        rebaseManager.rebaseBranch(git, ref)
    }

    fun selectUncommitedChanges() {
        tabState.newSelectedItem(SelectedItem.UncommitedChanges)

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

    fun selectLogLine(commit: GraphNode) {
        tabState.newSelectedItem(SelectedItem.Commit(commit))

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
        } else
            _logSearchFilterResults.value = LogSearch.SearchResults(emptyList(), NONE_MATCHING_INDEX)
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

    fun rebaseInteractive(revCommit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE
    ) {
        tabState.emitNewTaskEvent(TaskEvent.RebaseInteractive(revCommit))
    }

    fun deleteRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        remoteOperationsManager.deleteBranch(git, branch)
    }
}

sealed class LogStatus {
    object Loading : LogStatus()
    class Loaded(
        val hasUncommitedChanges: Boolean,
        val plotCommitList: GraphCommitList,
        val currentBranch: Ref?,
        val statusSummary: StatusSummary,
        val commitsLimit: Int,
    ) : LogStatus()
}

sealed class LogSearch {
    object NotSearching : LogSearch()
    data class SearchResults(
        val commits: List<GraphNode>,
        val index: Int,
        val totalCount: Int = commits.count(),
    ) : LogSearch()
}
