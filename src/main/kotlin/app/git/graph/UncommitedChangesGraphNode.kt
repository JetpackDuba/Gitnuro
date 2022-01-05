package app.git.graph

import org.eclipse.jgit.lib.ObjectId

class UncommitedChangesGraphNode : GraphNode(ObjectId(0, 0, 0, 0, 0)) {

    var graphParent: GraphNode? = null

    override val graphParentCount: Int
        get() = 1 // TODO: Check what happens with an empty tree

    override fun getGraphParent(nth: Int): GraphNode {
        return requireNotNull(graphParent)
    }
}