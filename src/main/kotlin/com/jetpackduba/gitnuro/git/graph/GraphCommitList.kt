package com.jetpackduba.gitnuro.git.graph

import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.revwalk.RevCommit
import java.util.*

/**
 * An ordered list of [GraphNode] subclasses.
 *
 *
 * Commits are allocated into lanes as they enter the list, based upon their
 * connections between descendant (child) commits and ancestor (parent) commits.
 *
 *
 * The source of the list must be a [GraphWalk]
 * and [.fillTo] must be used to populate the list.
 *
 * type of lane used by the application.
</L> */
class GraphCommitList(
    private val commits: MutableList<GraphNode> = mutableListOf(),
) : MutableList<GraphNode> by commits {
    var walker: GraphWalk? = null
    private var positionsAllocated = 0
    private val freePositions = TreeSet<Int>()
    private val activeLanes = HashSet<GraphLane>(32)

    private var parentId: AnyObjectId? = null

    private val graphCommit = UncommittedChangesGraphNode()

    var maxLine = 0
        private set

    /** number of (child) commits on a lane  */
    private val laneLength = HashMap<GraphLane, Int?>(
        32
    )

    override fun clear() {
        positionsAllocated = 0
        freePositions.clear()
        activeLanes.clear()
        laneLength.clear()
        commits.clear()
    }

    fun fillTo(highMark: Int) {
        if (this.size <= highMark) {
            var c: GraphNode? = null

            do {
                c = this.walker?.next()

                if (c != null) {
                    val stashChild = c.children.firstOrNull { it.isStash }

                    if (
                        stashChild == null ||
                        stashChild.parents.firstOrNull() == c
                    ) {
                        this.enter(this.size, c)
                        this.add(c)
                    }
                }
            } while (c != null && size <= highMark)

            if (c == null) {
                this.walker = null
            }
        }
    }

    fun addUncommittedChangesGraphCommit(parent: RevCommit) {
        parentId = parent.id
        graphCommit.lane = nextFreeLane()
    }

    fun enter(index: Int, currCommit: GraphNode) {
        var isUncommittedChangesNodeParent = false
        if (currCommit.id == parentId) {
            graphCommit.graphParent = currCommit
            currCommit.addChild(graphCommit, addFirst = true)
            isUncommittedChangesNodeParent = true
        }

        setupChildren(currCommit)
        val nChildren = currCommit.childCount
        if (nChildren == 0) {
            currCommit.lane = nextFreeLane()
        } else if (nChildren == 1
            && currCommit.children[0].graphParentCount < 2
        ) {
            // Only one child, child has only us as their parent.
            // Stay in the same lane as the child.
            val graphNode: GraphNode = currCommit.children[0]
            currCommit.lane = graphNode.lane
            var len = laneLength[currCommit.lane]
            len = if (len != null) Integer.valueOf(len.toInt() + 1) else Integer.valueOf(0)

            if (currCommit.lane.position != INVALID_LANE_POSITION)
                laneLength[currCommit.lane] = len
        } else {
            // We look for the child lane the current commit should continue.
            // Candidate lanes for this are those with children, that have the
            // current commit as their first parent.
            // There can be multiple candidate lanes. In that case the longest
            // lane is chosen, as this is usually the lane representing the
            // branch the commit actually was made on.

            // When there are no candidate lanes (i.e. the current commit has
            // only children whose non-first parent it is) we place the current
            // commit on a new lane.

            // The lane the current commit will be placed on:
            var reservedLane: GraphLane? = null
            var childOnReservedLane: GraphNode? = null
            var lengthOfReservedLane = -1


            if (isUncommittedChangesNodeParent) {
                val length = laneLength[graphCommit.lane]
                if (length != null) {
                    reservedLane = graphCommit.lane
                    childOnReservedLane = graphCommit
                    lengthOfReservedLane = length
                }
            } else {
                val children = currCommit.children.sortedBy { it.lane.position }
                for (child in children) {
                    if (child.getGraphParent(0) === currCommit) {
                        if (child.lane.position < 0)
                            println("child.lane.position is invalid (${child.lane.position})")

                        val length = laneLength[child.lane]

                        // we may be the first parent for multiple lines of
                        // development, try to continue the longest one
                        if (length != null && length > lengthOfReservedLane) {
                            reservedLane = child.lane
                            childOnReservedLane = child
                            lengthOfReservedLane = length

                            break
                        }
                    }
                }
            }
            if (reservedLane != null) {
                currCommit.lane = reservedLane
                laneLength[reservedLane] = Integer.valueOf(lengthOfReservedLane + 1)
                handleBlockedLanes(index, currCommit, childOnReservedLane)
            } else {
                currCommit.lane = nextFreeLane()
                handleBlockedLanes(index, currCommit, null)
            }

            // close lanes of children, if there are no first parents that might
            // want to continue the child lanes
            for (i in 0 until nChildren) {
                val graphNode = currCommit.children[i]

                val firstParent = graphNode.getGraphParent(0)

                if (firstParent.lane.position != INVALID_LANE_POSITION && firstParent.lane !== graphNode.lane)
                    closeLane(graphNode.lane)
            }
        }

        continueActiveLanes(currCommit)

        if (currCommit.parentCount == 0 && currCommit.lane.position == INVALID_LANE_POSITION) {
            closeLane(currCommit.lane)
        }

        if (
            currCommit.childCount == 1 &&
            currCommit.children.first().isStash &&
            currCommit.parentCount == 0
        ) {
            closeLane(currCommit.lane)
        }
    }

    private fun continueActiveLanes(currCommit: GraphNode) {
        for (lane in activeLanes) {
            if (lane !== currCommit.lane) {
                currCommit.addPassingLane(lane)
            }
        }
    }

    /**
     * Sets up fork and merge information in the involved PlotCommits.
     * Recognizes and handles blockades that involve forking or merging arcs.
     *
     * @param index
     * the index of `currCommit` in the list
     * @param currentNode
     * @param childOnLane
     * the direct child on the same lane as `currCommit`,
     * may be null if `currCommit` is the first commit on
     * the lane
     */
    private fun handleBlockedLanes(
        index: Int,
        currentNode: GraphNode,
        childOnLane: GraphNode?,
    ) {
        for (child in currentNode.children) {
            if (child === childOnLane) continue  // simple continuations of lanes are handled by
            // continueActiveLanes() calls in enter()

            // Is the child a merge or is it forking off?
            val childIsMerge = child.getGraphParent(0) !== currentNode
            if (childIsMerge) {
                var laneToUse = currentNode.lane
                laneToUse = handleMerge(
                    index, currentNode, childOnLane, child,
                    laneToUse
                )
                child.addMergingLane(laneToUse)
            } else {
                // We want to draw a forking arc in the child's lane.
                // As an active lane, the child lane already continues
                // (unblocked) up to this commit, we only need to mark it as
                // forking off from the current commit.
                val laneToUse = child.lane
                currentNode.addForkingOffLane(laneToUse)
            }
        }
    }

    // Handles the case where currCommit is a non-first parent of the child
    private fun handleMerge(
        index: Int,
        currCommit: GraphNode,
        childOnLane: GraphNode?,
        child: GraphNode,
        laneToUse: GraphLane,
    ): GraphLane {

        // find all blocked positions between currCommit and this child
        var newLaneToUse = laneToUse
        var childIndex = index // useless initialization, should
        // always be set in the loop below
        val blockedPositions = BitSet()
        for (r in index - 1 downTo 0) {
            val graphNode: GraphNode? = get(r)
            if (graphNode === child) {
                childIndex = r
                break
            }
            addBlockedPosition(blockedPositions, graphNode)
        }

        // handle blockades
        if (blockedPositions[newLaneToUse.position]) {
            // We want to draw a merging arc in our lane to the child,
            // which is on another lane, but our lane is blocked.

            // Check if childOnLane is beetween commit and the child we
            // are currently processing
            var needDetour = false
            if (childOnLane != null) {
                for (r in index - 1 downTo childIndex + 1) {
                    val graphNode: GraphNode? = get(r)
                    if (graphNode === childOnLane) {
                        needDetour = true
                        break
                    }
                }
            }
            if (needDetour) {
                // It is childOnLane which is blocking us. Repositioning
                // our lane would not help, because this repositions the
                // child too, keeping the blockade.
                // Instead, we create a "detour lane" which gets us
                // around the blockade. That lane has no commits on it.
                newLaneToUse = nextFreeLane(blockedPositions)
                currCommit.addForkingOffLane(newLaneToUse)
                closeLane(newLaneToUse)
            } else {
                // The blockade is (only) due to other (already closed)
                // lanes at the current lane's position. In this case we
                // reposition the current lane.
                // We are the first commit on this lane, because
                // otherwise the child commit on this lane would have
                // kept other lanes from blocking us. Since we are the
                // first commit, we can freely reposition.
                val newPos = getFreePosition(blockedPositions)
                freePositions.add(newLaneToUse.position)

                newLaneToUse.position = newPos
            }
        }

        // Actually connect currCommit to the merge child
        drawLaneToChild(index, child, newLaneToUse)
        return newLaneToUse
    }

    /**
     * Connects the commit at commitIndex to the child, using the given lane.
     * All blockades on the lane must be resolved before calling this method.
     *
     * @param commitIndex
     * @param child
     * @param laneToContinue
     */
    private fun drawLaneToChild(
        commitIndex: Int, child: GraphNode,
        laneToContinue: GraphLane,
    ) {
        for (index in commitIndex - 1 downTo 0) {
            val graphNode: GraphNode? = get(index)
            if (graphNode === child) break
            graphNode?.addPassingLane(laneToContinue)
        }
    }

    private fun closeLane(lane: GraphLane) {
        if (activeLanes.remove(lane)) {
            laneLength.remove(lane)
            freePositions.add(Integer.valueOf(lane.position))
        }
    }

    private fun setupChildren(currCommit: GraphNode) {
        for (parent in currCommit.parents) {
            (parent as GraphNode).addChild(currCommit)
        }
    }

    private fun nextFreeLane(blockedPositions: BitSet? = null): GraphLane {
        val newPlotLane = GraphLane(position = getFreePosition(blockedPositions))
        activeLanes.add(newPlotLane)
        laneLength[newPlotLane] = Integer.valueOf(1)

        return newPlotLane
    }

    /**
     * @param blockedPositions
     * may be null
     * @return a free lane position
     */
    private fun getFreePosition(blockedPositions: BitSet?): Int {
        if (freePositions.isEmpty()) return positionsAllocated++
        if (blockedPositions != null) {
            for (pos in freePositions) if (!blockedPositions[pos]) {
                freePositions.remove(pos)
                return pos
            }
            return positionsAllocated++
        }
        val min = freePositions.first()
        freePositions.remove(min)
        return min.toInt()
    }

    private fun addBlockedPosition(
        blockedPositions: BitSet,
        graphNode: GraphNode?,
    ) {
        if (graphNode != null) {
            val lane = graphNode.lane

            // Positions may be blocked by a commit on a lane.
            if (lane.position != INVALID_LANE_POSITION) {
                blockedPositions.set(lane.position)
            }

            // Positions may also be blocked by forking off and merging lanes.
            // We don't consider passing lanes, because every passing lane forks
            // off and merges at it ends.
            for (graphLane in graphNode.forkingOffLanes) {
                blockedPositions.set(graphLane.position)
            }

            for (graphLane in graphNode.mergingLanes) {
                blockedPositions.set(graphLane.position)
            }
        }
    }

    fun calcMaxLine() {
        if (this.isNotEmpty()) {
            maxLine = this.maxOf { it.lane.position }
        }
    }
}
