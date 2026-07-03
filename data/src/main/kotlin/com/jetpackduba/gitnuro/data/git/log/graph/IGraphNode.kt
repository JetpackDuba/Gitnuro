package com.jetpackduba.gitnuro.data.git.log.graph

interface IGraphNode {
    val graphParentCount: Int
    fun getGraphParent(nth: Int): GraphNode
}