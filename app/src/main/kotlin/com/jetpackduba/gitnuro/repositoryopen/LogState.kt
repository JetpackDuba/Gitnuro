package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import com.jetpackduba.gitnuro.domain.models.StatusSummary
import kotlinx.coroutines.flow.Flow

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

fun combineLogState(
    log: Flow<GraphCommits>,
    hasUncommittedChanges: Flow<Boolean>,
    currentBranch: Flow<Branch?>,
    statusSummary: Flow<StatusSummary>,
    logSearchFilterResults: Flow<LogSearch>,
    verticalListState: Flow<LazyListState>,
    horizontalListState: Flow<ScrollState>,
): Flow<LogState> {
    return combine(
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
}