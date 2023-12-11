package com.jetpackduba.gitnuro.git.graph

import org.eclipse.jgit.lib.ObjectId

class UncommittedChangesGraphNode : GraphNode(ObjectId(0, 0, 0, 0, 0)) {

    var graphParent: GraphNode? = null

    override val graphParentCount: Int
        get() = 1 // Uncommitted changes can have a max of 1 parent commit

    override fun getGraphParent(nth: Int): GraphNode {
        return requireNotNull(graphParent)
    }
}