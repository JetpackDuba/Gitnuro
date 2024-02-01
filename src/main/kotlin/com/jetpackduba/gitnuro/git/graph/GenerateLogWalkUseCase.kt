package com.jetpackduba.gitnuro.git.graph

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject


class GenerateLogWalkUseCase @Inject constructor() {
    suspend operator fun invoke(
        git: Git,
        firstCommit: RevCommit,
        allRefs: List<Ref>,
        stashes: List<RevCommit>,
        hasUncommittedChanges: Boolean,
        commitsLimit: Int?,
    ): GraphCommitList2 = withContext(Dispatchers.IO) {
        val reservedLanes = mutableMapOf<Int, String>()
        val graphNodes = mutableListOf<GraphNode2>()
        var maxLane = 0

        val availableCommitsToAdd = mutableMapOf<String, RevCommit>()

        val refsWithCommits = allRefs.map {
            val commit = git.repository.parseCommit(it.objectId)

            commit to it
        }

        val commitsOfRefs = refsWithCommits.map {
            it.first.name to it.first
        }

        val commitsOfStashes = stashes.map { it.name to it }

        availableCommitsToAdd[firstCommit.name] = firstCommit
        availableCommitsToAdd.putAll(commitsOfRefs)
        availableCommitsToAdd.putAll(commitsOfStashes)

        var currentCommit = getNextCommit(availableCommitsToAdd.values.toList())

        if (hasUncommittedChanges) {
            reservedLanes[0] = firstCommit.name
        }

        availableCommitsToAdd.remove(currentCommit?.name)

        while (currentCommit != null && (commitsLimit == null || graphNodes.count() <= commitsLimit)) {
            val lanes = getReservedLanes(reservedLanes, currentCommit.name)
            val lane = lanes.first()
            val forkingLanes = lanes - lane
            val isStash = stashes.any { it == currentCommit }

            val parents = sortParentsByPriority(git, currentCommit)
                .filterStashParentsIfRequired(isStash)

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

            val refs = refsByCommit(refsWithCommits, currentCommit)

            val graphNode = createGraphNode(
                currentCommit = currentCommit,
                isStash = isStash,
                lane = lane,
                forkingLanes = forkingLanes,
                reservedLanes = reservedLanes,
                mergingLanes = mergingLanes,
                refs = refs
            )

            if (lane > maxLane) {
                maxLane = lane
            }

            graphNodes.add(graphNode)
            removeFromAllLanes(reservedLanes, graphNode.name)

            availableCommitsToAdd.putAll(parents.map { it.name to it })

            currentCommit = getNextCommit(availableCommitsToAdd.values.toList())
            availableCommitsToAdd.remove(currentCommit?.name)
        }

        GraphCommitList2(graphNodes, maxLane)
    }

    private fun createGraphNode(
        currentCommit: RevCommit,
        isStash: Boolean,
        lane: Int,
        forkingLanes: List<Int>,
        reservedLanes: MutableMap<Int, String>,
        mergingLanes: MutableList<Int>,
        refs: List<Ref>
    ) = GraphNode2(
        currentCommit.name,
        currentCommit.shortMessage,
        currentCommit.fullMessage,
        currentCommit.authorIdent,
        currentCommit.committerIdent,
        currentCommit.parentCount,
        isStash = isStash,
        lane = lane,
        forkingLanes = forkingLanes,
        passingLanes = reservedLanes.keys.toList() - mergingLanes.toSet() - forkingLanes.toSet(),
        mergingLanes = mergingLanes,
        refs = refs,
    )

    private fun refsByCommit(
        refsWithCommits: List<Pair<RevCommit, Ref>>,
        commit: RevCommit
    ): List<Ref> = refsWithCommits
        .filter { it.first == commit }
        .map { it.second }

    private fun sortParentsByPriority(git: Git, currentCommit: RevCommit): List<RevCommit> {
        val parents = currentCommit
            .parents
            .map { git.repository.parseCommit(it) }
            .toMutableList()

        return if (parents.count() <= 1) {
            parents
        } else if (parents.count() == 2) {
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
    }

    fun List<RevCommit>.filterStashParentsIfRequired(isStash: Boolean): List<RevCommit> {
        return if (isStash) {
            filterNot {
                it.shortMessage.startsWith("index on") ||
                        it.shortMessage.startsWith("untracked files on")
            }
        } else {
            this
        }
    }

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

sealed interface ReservationType {
    val hash: String

    class ParentInSameLane(override val hash: String): ReservationType
    class ParentInVariableLane(override val hash: String): ReservationType
}