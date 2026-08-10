package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable
import java.util.*

@Immutable
data class GraphCommits(
    val commits: LinkedHashMap<String, GraphCommit> = LinkedHashMap(),
    val maxLane: Int = 0,
): SequencedMap<String, GraphCommit> by commits