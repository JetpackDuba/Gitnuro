package com.jetpackduba.gitnuro.repositoryopen

import com.jetpackduba.gitnuro.domain.models.*

sealed interface LogSearch {
    data object NotSearching : LogSearch
    data class SearchResults(
        val commits: List<GraphCommit>,
        val index: Int,
        val totalCount: Int = commits.count(),
    ) : LogSearch
}


