package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.TaskEvent
import com.jetpackduba.gitnuro.git.branches.*
import com.jetpackduba.gitnuro.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.git.graph.GraphNode
import com.jetpackduba.gitnuro.git.log.*
import com.jetpackduba.gitnuro.git.rebase.GetRebaseLinesFullMessageUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.DeleteRemoteBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PullFromSpecificBranchUseCase
import com.jetpackduba.gitnuro.git.remote_operations.PushToSpecificBranchUseCase
import com.jetpackduba.gitnuro.git.tags.CreateTagOnCommitUseCase
import com.jetpackduba.gitnuro.git.tags.DeleteTagUseCase
import com.jetpackduba.gitnuro.git.workspace.CheckHasUncommitedChangedUseCase
import com.jetpackduba.gitnuro.git.workspace.GetStatusSummaryUseCase
import com.jetpackduba.gitnuro.git.workspace.StatusSummary
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.log.LogDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.lib.RebaseTodoLine
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
    private val checkHasUncommitedChangedUseCase: CheckHasUncommitedChangedUseCase,
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
    private val createBranchOnCommitUseCase: CreateBranchOnCommitUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val pushToSpecificBranchUseCase: PushToSpecificBranchUseCase,
    private val pullFromSpecificBranchUseCase: PullFromSpecificBranchUseCase,
    private val deleteRemoteBranchUseCase: DeleteRemoteBranchUseCase,
    private val checkoutCommitUseCase: CheckoutCommitUseCase,
    private val revertCommitUseCase: RevertCommitUseCase,
    private val resetToCommitUseCase: ResetToCommitUseCase,
    private val cherryPickCommitUseCase: CherryPickCommitUseCase,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val createTagOnCommitUseCase: CreateTagOnCommitUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val getRebaseLinesFullMessageUseCase: GetRebaseLinesFullMessageUseCase,
    private val tabState: TabState,
    private val appSettings: AppSettings,
    private val tabScope: CoroutineScope,
) : ViewModel {
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

    val scrollToUncommitedChanges: Flow<SelectedItem.UncommitedChanges> = tabState.taskEvent
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
            appSettings.commitsLimitEnabledFlow.drop(1).collectLatest {
                tabState.refreshData(RefreshType.ONLY_LOG)
            }
        }
        tabScope.launch {
            appSettings.commitsLimitFlow.collectLatest {
                tabState.refreshData(RefreshType.ONLY_LOG)
            }
        }

        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.ALL_DATA,
                RefreshType.ONLY_LOG,
                RefreshType.UNCOMMITED_CHANGES,
                RefreshType.UNCOMMITED_CHANGES_AND_LOG,
            ) { refreshType ->
                if (refreshType == RefreshType.UNCOMMITED_CHANGES) {
                    uncommitedChangesLoadLog(tabState.git)
                } else
                    refresh(tabState.git)
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

        val hasUncommitedChanges = statusSummary.total > 0
        val commitsLimit = if (appSettings.commitsLimitEnabled) {
            appSettings.commitsLimit
        } else
            Int.MAX_VALUE

        val commitsLimitDisplayed = if (appSettings.commitsLimitEnabled) {
            appSettings.commitsLimit
        } else
            -1

        val log = getLogUseCase(git, currentBranch, hasUncommitedChanges, commitsLimit)

        _logStatus.value =
            LogStatus.Loaded(hasUncommitedChanges, log, currentBranch, statusSummary, commitsLimitDisplayed)

        // Remove search filter if the log has been updated
        _logSearchFilterResults.value = LogSearch.NotSearching
    }


    fun pushToRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        pushToSpecificBranchUseCase(
            git = git,
            force = false,
            pushTags = false,
            remoteBranch = branch,
        )
    }

    fun pullFromRemoteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        pullFromSpecificBranchUseCase(
            git = git,
            rebase = false,
            remoteBranch = branch,
        )
    }

    fun checkoutCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        checkoutCommitUseCase(git, revCommit)
    }

    fun revertCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashes = true,
    ) { git ->
        revertCommitUseCase(git, revCommit)
    }

    fun resetToCommit(revCommit: RevCommit, resetType: ResetType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        resetToCommitUseCase(git, revCommit, resetType = resetType)
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        checkoutRefUseCase(git, ref)
    }

    fun cherrypickCommit(revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        cherryPickCommitUseCase(git, revCommit)
    }

    fun createBranchOnCommit(branch: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        refreshEvenIfCrashesInteractive = { it is CheckoutConflictException },
    ) { git ->
        createBranchOnCommitUseCase(git, branch, revCommit)
    }

    fun createTagOnCommit(tag: String, revCommit: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        createTagOnCommitUseCase(git, tag, revCommit)
    }

    fun mergeBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        mergeBranchUseCase(git, ref, appSettings.ffMerge)
    }

    fun deleteBranch(branch: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        deleteBranchUseCase(git, branch)
    }

    fun deleteTag(tag: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        deleteTagUseCase(git, tag)
    }

    suspend fun refreshUncommitedChanges(git: Git) {
        uncommitedChangesLoadLog(git)
    }

    private suspend fun uncommitedChangesLoadLog(git: Git) {
        val currentBranch = getCurrentBranchUseCase(git)
        val hasUncommitedChanges = checkHasUncommitedChangedUseCase(git)

        val statsSummary = if (hasUncommitedChanges) {
            getStatusSummaryUseCase(
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
        rebaseBranchUseCase(git, ref)
    }

    fun selectUncommitedChanges() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
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

    fun squashCommits() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        val selectedItem = tabState.selectedItem.value
        val log = logStatus.value

        if (selectedItem is SelectedItem.MultiCommitBasedItem && log is LogStatus.Loaded) {
            val firstCommit = selectedItem.itemList
                .sortedBy { it.commitTime }
                .minBy { it.commitTime }

            val firstCommitIndex = log.plotCommitList.indexOf(firstCommit)
            val upstreamCommit = log.plotCommitList[firstCommitIndex + 1]

            tabState.emitNewTaskEvent(
                TaskEvent.SquashCommits(
                    commits = selectedItem.itemList,
                    upstreamCommit = upstreamCommit
                )
            )
        }
    }

    fun selectLogLine(
        commit: GraphNode,
        multiSelect: Boolean,
        rangeSelect: Boolean
    ) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        when {
            multiSelect -> selectMultiLogLines(commit)
            rangeSelect -> selectRangeLogLines(commit)
            else -> selectSingleLogLine(commit)
        }

        setLogSearchFilterByCommit(commit)
    }

    // like with ctrl pressed
    private suspend fun selectMultiLogLines(commit: GraphNode) {
        when (val selectedItem = tabState.selectedItem.value) {
            is SelectedItem.None,
            is SelectedItem.UncommitedChanges -> selectSingleLogLine(commit)
            is SelectedItem.CommitBasedItem -> {
                if (selectedItem.revCommit == commit) {
                    tabState.noneSelected()
                } else {
                    val list = listOf(selectedItem.revCommit, commit)
                    tabState.newSelectedItem(SelectedItem.MultiCommitBasedItem(list, selectedItem.revCommit))
                }
            }
            is SelectedItem.MultiCommitBasedItem -> {
                val revList = selectedItem.itemList
                val list = if (revList.contains(commit)) {
                    revList - commit
                } else {
                    revList + commit
                }

                val item = if (list.size > 1) {
                    SelectedItem.MultiCommitBasedItem(list, list.maxBy { it.commitTime })
                } else {
                    SelectedItem.Commit(list.first())
                }

                tabState.newSelectedItem(item)
            }
        }
    }

    // like with shift pressed
    private suspend fun selectRangeLogLines(commit: GraphNode) {
        when (val selectedItem = tabState.selectedItem.value) {
            is SelectedItem.None,
            is SelectedItem.UncommitedChanges -> selectSingleLogLine(commit)
            is SelectedItem.CommitBasedItem -> {
                val list = getRangeCommitsFromOneToOne(selectedItem.revCommit, commit)
                tabState.newSelectedItem(SelectedItem.MultiCommitBasedItem(list, selectedItem.revCommit))
            }
            is SelectedItem.MultiCommitBasedItem -> {
                val list = getRangeCommitsFromOneToOne(selectedItem.targetCommit, commit)
                tabState.newSelectedItem(SelectedItem.MultiCommitBasedItem(list, selectedItem.targetCommit))
            }
        }
    }

    private fun getRangeCommitsFromOneToOne(from: RevCommit, to: RevCommit): List<RevCommit> {
        return if (from != to && logStatus.value is LogStatus.Loaded) {
            val commitList = (logStatus.value as LogStatus.Loaded).plotCommitList

            val first = commitList.indexOf(from)
            val last = commitList.indexOf(to)
            val range = if (first < last) first.rangeTo(last) else last.rangeTo(first)

            println(range)

            commitList.slice(range)
        } else {
            listOf(from)
        }
    }

    private suspend fun selectSingleLogLine(commit: GraphNode) {
        tabState.newSelectedItem(SelectedItem.Commit(commit))
    }

    private fun setLogSearchFilterByCommit(commit: GraphNode) {
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
        deleteRemoteBranchUseCase(git, branch)
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
