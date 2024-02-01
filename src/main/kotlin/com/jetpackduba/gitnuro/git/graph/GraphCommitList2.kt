package com.jetpackduba.gitnuro.git.graph

data class GraphCommitList2(
    val nodes: List<GraphNode2>,
    val maxLane: Int
) : List<GraphNode2> by nodes