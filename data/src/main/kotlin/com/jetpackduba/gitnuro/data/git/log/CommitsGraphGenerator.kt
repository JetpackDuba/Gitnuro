package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.data.mappers.GraphCommitMapper
import com.jetpackduba.gitnuro.domain.git.graph.GraphLane
import com.jetpackduba.gitnuro.domain.git.graph.GraphNode
import com.jetpackduba.gitnuro.domain.git.graph.GraphWalk
import com.jetpackduba.gitnuro.domain.git.graph.INVALID_LANE_POSITION
import com.jetpackduba.gitnuro.domain.git.graph.UncommittedChangesGraphNode
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.revwalk.RevCommit
import java.util.BitSet
import java.util.LinkedHashMap

class CommitsGraphGenerator(
    private val walker: GraphWalk,
    private val previousData: GraphCommits?,
    private val commitMapper: GraphCommitMapper,
) {
    private val data = previousData ?: GraphCommits(commits = LinkedHashMap(), maxLane = 0)
    private val commits = LinkedHashMap(data.commits)
    private var maxLane = data.maxLane
    private var parentId: AnyObjectId? = null

    private val graphCommit = UncommittedChangesGraphNode()

    fun generateUpTo(maxNumberOfCommits: Int): GraphCommits {
        return if (commits.count() <= maxNumberOfCommits) {
            var c: GraphNode? = null

            do {
                c = this.walker.next()

                if (c != null) {
                    val stashChild = c.children.firstOrNull { it.isStash }

                    if (
                        stashChild == null ||
                        stashChild.parents.firstOrNull() == c
                    ) {
                        this.enter(commits.count(), c)
                        commits[c.name] = commitMapper.toDomain(c)
                    }
                }
            } while (c != null && commits.count() <= maxNumberOfCommits)

            GraphCommits(
                commits,
                maxLane,
            )
        } else {
            data
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
            var len = walker.laneLength[currCommit.lane]
            len = if (len != null) Integer.valueOf(len + 1) else Integer.valueOf(0)

            if (currCommit.lane.position != INVALID_LANE_POSITION)
                walker.laneLength[currCommit.lane] = len
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
                val length = walker.laneLength[graphCommit.lane]
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

                        val length = walker.laneLength[child.lane]

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
                walker.laneLength[reservedLane] = Integer.valueOf(lengthOfReservedLane + 1)
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
        for (lane in walker.activeLanes) {
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

                val graphCommit = commits[child.name]

                if (graphCommit != null) {
                    commits[child.name] = graphCommit.copy(mergingLanes = graphCommit.mergingLanes + laneToUse.position)
                }
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
        val commits = commits.toList()
        for (r in index - 1 downTo 0) {
            val graphCommit = commits.elementAtOrNull(r)?.second
            if (graphCommit?.hash == child.name) {
                childIndex = r
                break
            }
            addBlockedPosition(blockedPositions, graphCommit)
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
                    val graphCommit = commits.elementAtOrNull(r)?.second
                    if (graphCommit?.hash == child.name) {
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
                walker.freePositions.add(newLaneToUse.position)

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
        commitIndex: Int,
        child: GraphNode,
        laneToContinue: GraphLane,
    ) {
        for (index in commitIndex - 1 downTo 0) {
            val commits = commits.toList()
            val graphCommit = commits.elementAtOrNull(index)?.second
            if (graphCommit?.hash == child.name) break

            if (graphCommit != null) {
                val newGraphCommit = graphCommit.copy(passingLanes = graphCommit.passingLanes + laneToContinue.position)
                this.commits[graphCommit.hash] = newGraphCommit
            }
        }
    }

    private fun closeLane(lane: GraphLane) {
        if (walker.activeLanes.remove(lane)) {
            walker.laneLength.remove(lane)
            walker.freePositions.add(Integer.valueOf(lane.position))
        }
    }

    private fun setupChildren(currCommit: GraphNode) {
        for (parent in currCommit.parents) {
            (parent as GraphNode).addChild(currCommit)
        }
    }

    private fun nextFreeLane(blockedPositions: BitSet? = null): GraphLane {
        val newPlotLane = GraphLane(position = getFreePosition(blockedPositions))
        walker.activeLanes.add(newPlotLane)
        walker.laneLength[newPlotLane] = Integer.valueOf(1)

        return newPlotLane
    }

    /**
     * @param blockedPositions
     * may be null
     * @return a free lane position
     */
    private fun getFreePosition(blockedPositions: BitSet?): Int {
        if (walker.freePositions.isEmpty()) return walker.positionsAllocated++
        if (blockedPositions != null) {
            for (pos in walker.freePositions) if (!blockedPositions[pos]) {
                walker.freePositions.remove(pos)
                return pos
            }
            return walker.positionsAllocated++
        }
        val min = walker.freePositions.first()
        walker.freePositions.remove(min)
        return min.toInt()
    }

    private fun addBlockedPosition(
        blockedPositions: BitSet,
        graphNode: GraphCommit?,
    ) {
        if (graphNode != null) {
            val lane = graphNode.lane

            // Positions may be blocked by a commit on a lane.
            if (lane != INVALID_LANE_POSITION) {
                blockedPositions.set(lane)
            }

            // Positions may also be blocked by forking off and merging lanes.
            // We don't consider passing lanes, because every passing lane forks
            // off and merges at it ends.
            for (graphLane in graphNode.forkingOffLanes) {
                blockedPositions.set(graphLane)
            }

            for (graphLane in graphNode.mergingLanes) {
                blockedPositions.set(graphLane)
            }
        }
    }
}