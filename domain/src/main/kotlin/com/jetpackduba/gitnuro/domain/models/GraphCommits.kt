package com.jetpackduba.gitnuro.domain.models

data class GraphCommits(
    val commits: List<GraphCommit>,
    val maxLane: Int,
): List<GraphCommit> by commits