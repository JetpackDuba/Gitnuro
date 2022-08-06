package app.git.graph

import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

private val NO_CHILDREN = arrayOf<GraphNode>()
private val NO_LANES = arrayOf<GraphLane>()
private val NO_LANE = GraphLane(INVALID_LANE_POSITION)
val NO_REFS = listOf<Ref>()

open class GraphNode(id: AnyObjectId?) : RevCommit(id), IGraphNode {
    var forkingOffLanes: Array<GraphLane> = NO_LANES
    var passingLanes: Array<GraphLane> = NO_LANES
    var mergingLanes: Array<GraphLane> = NO_LANES
    var lane: GraphLane = NO_LANE
    var children: Array<GraphNode> = NO_CHILDREN
    var refs: List<Ref> = NO_REFS

    fun addForkingOffLane(graphLane: GraphLane) {
        forkingOffLanes = addLane(graphLane, forkingOffLanes)
    }

    fun addPassingLane(graphLane: GraphLane) {
        passingLanes = addLane(graphLane, passingLanes)
    }

    fun addMergingLane(graphLane: GraphLane) {
        mergingLanes = addLane(graphLane, mergingLanes)
    }

    fun addChild(c: GraphNode, addFirst: Boolean = false) {
        when (val childrenCount = children.count()) {
            0 -> children = arrayOf(c)
            1 -> {
                if (!c.id.equals(children[0].id)) {
                    children = if (addFirst) {
                        arrayOf(c, children[0])
                    } else
                        arrayOf(children[0], c)
                }
            }
            else -> {
                for (pc in children)
                    if (c.id.equals(pc.id))
                        return

                val resultArray = if (addFirst) {
                    val childList = mutableListOf(c)
                    childList.addAll(children)
                    childList.toTypedArray()
                } else {
                    children.copyOf(childrenCount + 1).run {
                        this[childrenCount] = c
                        requireNoNulls()
                    }
                }

                children = resultArray
            }
        }
    }

    val childCount: Int
        get() {
            return children.size
        }

    override fun reset() {
        forkingOffLanes = NO_LANES
        passingLanes = NO_LANES
        mergingLanes = NO_LANES
        children = NO_CHILDREN
        lane = NO_LANE
        super.reset()
    }

    private fun addLane(graphLane: GraphLane, lanes: Array<GraphLane>): Array<GraphLane> {
        var newLines = lanes

        when (val linesCount = newLines.count()) {
            0 -> newLines = arrayOf(graphLane)
            1 -> newLines = arrayOf(newLines[0], graphLane)
            else -> {
                val n = newLines.copyOf(linesCount + 1).run {
                    this[linesCount] = graphLane
                    requireNoNulls()
                }

                newLines = n
            }
        }

        return newLines
    }

    override val graphParentCount: Int
        get() = parentCount

    override fun getGraphParent(nth: Int): GraphNode {
        return getParent(nth) as GraphNode
    }
}