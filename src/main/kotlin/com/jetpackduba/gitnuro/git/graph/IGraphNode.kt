package com.jetpackduba.gitnuro.git.graph

interface IGraphNode {
    val graphParentCount: Int
    fun getGraphParent(nth: Int): GraphNode
}