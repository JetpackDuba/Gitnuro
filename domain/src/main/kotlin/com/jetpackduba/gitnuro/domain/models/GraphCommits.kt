package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Stable
import java.util.*

@Stable
data class GraphCommits(
    val commits: LinkedHashMap<String, GraphCommit> = LinkedHashMap(),
    val maxLane: Int = 0,
): SequencedMap<String, GraphCommit> by commits