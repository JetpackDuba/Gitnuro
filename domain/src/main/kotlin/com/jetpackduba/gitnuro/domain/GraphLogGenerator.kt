package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.max

@TabScope
class GraphLogGenerator @Inject constructor(
    private val graphRevWalkerProvider: Provider<GraphRevWalker>,
) {
    // Key -> Reserved lane index. Value -> Commit that is reserved for
    private val reservedLanes = mutableMapOf<Int, String>()
    private val commitsToSkip = mutableSetOf<String>()

    suspend fun generate(
        repository: String,
        maxCommits: Int,
        hashes: List<String>,
        stashes: HashSet<String>,
        forcedFirstLaneBranchHash: String?,
        pagination: Pagination,
    ): GraphCommits {
        val graphRevWalker = graphRevWalkerProvider.get()
        val previousCommitsCount: Int

        when (pagination) {
            Pagination.None -> {
                graphRevWalker.prepare(repository, hashes)
                reservedLanes.clear()
                previousCommitsCount = 0
                // Reserving first lane only can happen when getting new data, not from pagination
                if (forcedFirstLaneBranchHash != null) {
                    reserve(forcedFirstLaneBranchHash, 0)
                }
            }

            is Pagination.Paginated -> {
                val startingPoints = reservedLanes.values
                val hashesNotAlreadyPresent = hashes.filter { !pagination.data.containsKey(it) }
                previousCommitsCount = pagination.data.count()
                graphRevWalker.prepare(repository, startingPoints + hashesNotAlreadyPresent)
            }
        }

        val commits = LinkedHashMap<String, GraphCommit>()
        var maxLane = 0

        graphRevWalker.use {
            for (commit in graphRevWalker.iterator()) {
                if (commitsToSkip.contains(commit.hash)) {
                    commitsToSkip.remove(commit.hash)
                    continue
                }

                val graphCommit = createCommit(commit, stashes, forcedFirstLaneBranchHash)

                commits[graphCommit.hash] = graphCommit
                maxLane = max(maxLane, graphCommit.lane)

                if ((previousCommitsCount + commits.count()) > maxCommits) break
            }
        }

        return when (pagination) {
            Pagination.None -> GraphCommits(commits, maxLane = maxLane)
            is Pagination.Paginated -> {
                val commits = LinkedHashMap(pagination.data).apply {
                    putAll(commits)
                }
                GraphCommits(commits, maxLane = maxLane)
            }
        }
    }

    private fun createCommit(
        commit: Commit,
        stashes: HashSet<String>,
        forcedFirstLaneBranchHash: String?
    ): GraphCommit {
        val reservedLanesForCommit = getReservedLanesForHash(commit.hash)

        val lane = if (reservedLanesForCommit.isEmpty()) {
            nextAvailableLane()
        } else {
            reservedLanesForCommit.first()
        }

        val forkingOffLanes = reservedLanesForCommit.drop(1)
        val passingLanes = this.reservedLanes.keys - reservedLanesForCommit.toSet()
        val mergingLanes = mutableListOf<Int>()

        if (commit.parentsHashes.isNotEmpty()) {
            reserve(commit.parentsHashes.first(), lane)

            val parentHashesWithoutFirst = commit.parentsHashes.drop(1)
            val isStash = stashes.contains(commit.hash)

            if (parentHashesWithoutFirst.isNotEmpty() && !isStash) {
                for (hash in parentHashesWithoutFirst) {
                    val reservedLanes = getReservedLanesForHash(hash, remove = false)

                    val lane = if (reservedLanes.isEmpty()) {
                        val newLane = nextAvailableLane(lane)
                        reserve(hash, newLane)

                        newLane
                    } else {
                        reservedLanes.first()
                    }

                    mergingLanes.add(lane)
                }
            } else if (isStash) {
                commitsToSkip.addAll(parentHashesWithoutFirst)
            }
        }


        val c = GraphCommit(
            commit = commit,
            lane = lane,
            passingLanes = passingLanes.toList(),
            mergingLanes = mergingLanes,
            forkingOffLanes = forkingOffLanes,
            childCount = if (forcedFirstLaneBranchHash == commit.hash) 1 else reservedLanesForCommit.count(),
        )
        return c
    }

    private fun getReservedLanesForHash(hash: String, remove: Boolean = true): List<Int> {
        val lanes = reservedLanes
            .filter { (_, value) ->
                value == hash
            }
            .keys
            .toList()
            .sorted()

        if (remove) {
            for (lane in lanes) {
                reservedLanes.remove(lane)
            }
        }

        return lanes
    }

    private fun reserve(hash: String, lane: Int) {
        reservedLanes[lane] = hash
    }

    private fun nextAvailableLane(min: Int = 0): Int {
        if (min == 0 && reservedLanes.isEmpty()) {
            return 0
        }

        var isAvailable = false
        var lane = min

        while (!isAvailable) {
            isAvailable = !reservedLanes.containsKey(lane)

            if (!isAvailable) {
                lane++
            }
        }

        return lane
    }
}

data class ReservedLane(
    val reservedFor: String,
    val reservedBy: String,
)

sealed interface Pagination {
    data object None : Pagination
    data class Paginated(val data: GraphCommits) : Pagination
}

interface GraphRevWalker : Iterable<Commit>, Closeable {
    suspend fun prepare(repository: String, startingCommits: List<String>): Either<Unit, GitError>
}