package com.jetpackduba.gitnuro.git.graph

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import java.util.*
import javax.inject.Inject


class GenerateLogWalkUseCase @Inject constructor() {
    suspend operator fun invoke(
        git: Git,
        firstCommit: RevCommit,
    ): GraphCommitList2 = withContext(Dispatchers.IO) {
        val reservedLanes = mutableMapOf<Int, String>()
        val graphNodes = mutableListOf<GraphNode2>()

        val availableCommitsToAdd = mutableMapOf<String, RevCommit>()

        var currentCommit: RevCommit? = firstCommit

        do {
            if (currentCommit == null) continue

            val lanes = getReservedLanes(reservedLanes, currentCommit.name)
            val lane = lanes.first()
            val forkingLanes = lanes - lane

            val parents = sortParentsByPriority(git, currentCommit)

            val parentsCount = parents.count()
            val mergingLanes = mutableListOf<Int>()
            if (parentsCount == 1) {
                reservedLanes[lane] = parents.first().name
            } else if (parentsCount > 1) {
                reservedLanes[lane] = parents.first().name

                for (i in 1 until parentsCount) {
                    val availableLane = firstAvailableLane(reservedLanes)
                    reservedLanes[availableLane] = parents[i].name
                    mergingLanes.add(availableLane)
                }
            }

            val graphNode = GraphNode2(
                currentCommit.name,
                currentCommit.shortMessage,
                currentCommit.fullMessage,
                currentCommit.authorIdent,
                currentCommit.committerIdent,
                currentCommit.parentCount,
                isStash = false,
                lane = lane,
                forkingLanes = forkingLanes,
                passingLanes = reservedLanes.keys.toList() - mergingLanes.toSet() - forkingLanes.toSet(),
                mergingLanes = mergingLanes,
            )

            graphNodes.add(graphNode)
            removeFromAllLanes(reservedLanes, graphNode.name)

            availableCommitsToAdd.putAll(parents.map { it.name to it })

            currentCommit = getNextCommit(availableCommitsToAdd.values.toList())
            availableCommitsToAdd.remove(currentCommit?.name)
        } while (currentCommit != null)

        GraphCommitList2(graphNodes)
    }

    private fun sortParentsByPriority(git: Git, currentCommit: RevCommit): List<RevCommit> {
        val parents = currentCommit
            .parents
            .map { git.repository.parseCommit(it) }
            .toMutableList()

        return if (parents.count() <= 1) {
            parents
        }
        else if (parents.count() == 2) {
            if (parents[0].parents.any { it.name == parents[1].name }) {
                listOf(parents[1], parents[0])
            } else if (parents[1].parents.any { it.name == parents[0].name }) {
                parents
            } else {
                parents // TODO Sort by longer tree or detect the origin branch
            }
        } else {
            parents.sortedBy { it.committerIdent.`when` }
        }

//        parents.sortedWith { o1, o2 ->
//            TODO("Not yet implemented")
//        }
//        val parentsWithPriority = mapOf<RevCommit, Int>()
//
//
//
//        if (currentCommit.name.startsWith("Merge")) {
//            val originMergeParent = parents.firstOrNull {
//                currentCommit.fullMessage.contains(it.shortMessage)
//            }
//
//            if (originMergeParent != null) {
//                (parents - currentCommit) + currentCommit // this will remove the currentItem and re-add it to the end
//            } else
//        }

    }


    fun t(commit1: RevCommit, commit2: RevCommit, repository: Repository): RevCommit? {
        return RevWalk(repository).use { walk ->
            walk.setRevFilter(RevFilter.MERGE_BASE)
            walk.markStart(commit1)
            walk.markStart(commit2)

            walk.next()
        }
    }
//    fun getShortestParent(parent1: RevCommit, parent2: RevCommit) {
//        val logParent1 = mutableListOf<String>(parent1.name)
//        val logParent2 = mutableListOf<String>(parent2.name)
//
//        var newParent1: RevCommit? = parent1
//        var newParent2: RevCommit? = parent2
//
//        while (newParent1 != null && newParent2 != null) {
//            newParent1.pa
//        }
//    }

    fun getNextCommit(availableCommits: List<RevCommit>): RevCommit? {
        return availableCommits.sortedByDescending { it.committerIdent.`when` }.firstOrNull()
    }

    fun getReservedLanes(reservedLanes: Map<Int, String>, hash: String): List<Int> {
        val reservedLanesFiltered = reservedLanes.entries
            .asSequence()
            .map { it.key to it.value }
            .filter { (key, value) -> value == hash }
            .sortedBy { it.first }
            .toList()

        return if (reservedLanesFiltered.isEmpty()) {
            listOf(firstAvailableLane(reservedLanes))
        } else {
            reservedLanesFiltered.map { it.first }
        }
    }

    fun removeFromAllLanes(reservedLanes: MutableMap<Int, String>, hash: String) {
        val lanes = reservedLanes.entries.filter { it.value == hash }

        for (lane in lanes) {
            reservedLanes.remove(lane.key)
        }
    }

    fun firstAvailableLane(reservedLanes: Map<Int, String>): Int {
        val sortedKeys = reservedLanes.keys.sorted()

        return if (sortedKeys.isEmpty() || sortedKeys.first() > 0) {
            0
        } else if (sortedKeys.count() == 1) {
            val first = sortedKeys.first()

            if (first == 0) {
                1
            } else {
                0
            }
        } else {
            sortedKeys.asSequence()
                .zipWithNext { a, b ->
                    if (b - a > 1)
                        a + 1
                    else
                        null
                }
                .filterNotNull()
                .firstOrNull() ?: (sortedKeys.max() + 1)
        }
    }
}