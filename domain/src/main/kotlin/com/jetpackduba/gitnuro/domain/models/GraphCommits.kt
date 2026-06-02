package com.jetpackduba.gitnuro.domain.models

import java.util.*

data class GraphCommits(
    val commits: LinkedHashMap<String, GraphCommit>,
    val maxLane: Int,
): SequencedMap<String, GraphCommit> by commits