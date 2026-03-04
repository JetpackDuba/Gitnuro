package com.jetpackduba.gitnuro.domain.git.graph

interface IGraphNode {
    val graphParentCount: Int
    fun getGraphParent(nth: Int): GraphNode
}