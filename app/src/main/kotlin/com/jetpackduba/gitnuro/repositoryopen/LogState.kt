package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.domain.models.*
import kotlinx.coroutines.flow.Flow

@Stable
data class LogState(
    val isLoading: Boolean,
    val hasUncommittedChanges: Boolean = false,
    val commitList: GraphCommits = GraphCommits(LinkedHashMap(), 0),
    val currentBranch: Branch? = null,
    val branches:Map<String, List<Branch>> = emptyMap(),
    val tags: Map<String, List<Tag>> = emptyMap(),
    val stashes:HashSet<String> = HashSet(),
    val statusSummary: StatusSummary = StatusSummary(0, 0, 0, 0),
    val searchFilter: LogSearch = LogSearch.NotSearching,
    val verticalScrollState: LazyListState = LazyListState(),
    val horizontalScrollState: ScrollState = ScrollState(0),
)

fun combineLogState(
    log: Flow<GraphCommits>,
    hasUncommittedChanges: Flow<Boolean>,
    currentBranch: Flow<Branch?>,
    branches: Flow<Map<String, List<Branch>>>,
    tags: Flow<Map<String, List<Tag>>>,
    stashes: Flow<HashSet<String>>,
    statusSummary: Flow<StatusSummary>,
    logSearchFilterResults: Flow<LogSearch>,
    verticalListState: Flow<LazyListState>,
    horizontalListState: Flow<ScrollState>,
): Flow<LogState> {
    return combine(
        log,
        hasUncommittedChanges,
        currentBranch,
        branches,
        tags,
        stashes,
        statusSummary,
        logSearchFilterResults,
        verticalListState,
        horizontalListState,
    ) { log,
        hasUncommittedChanges,
        currentBranch,
        branches,
        tags,
        stashes,
        statusSummary,
        logSearchFilterResults,
        verticalListState,
        horizontalListState ->
        LogState(
            isLoading = false,
            hasUncommittedChanges,
            log,
            currentBranch,
            branches,
            tags,
            stashes,
            statusSummary,
            logSearchFilterResults,
            verticalListState,
            horizontalListState,
        )
    }
}