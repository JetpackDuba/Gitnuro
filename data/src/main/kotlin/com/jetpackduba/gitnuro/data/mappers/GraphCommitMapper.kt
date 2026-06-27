package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.git.graph.GraphNode
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import javax.inject.Inject

class GraphCommitMapper @Inject constructor(
    private val commitMapper: JGitCommitMapper,
): DataMapper<GraphCommit, GraphNode> {
    override fun toData(value: GraphCommit): Nothing {
        TODO("Not yet implemented")
    }

    override fun toDomain(value: GraphNode): GraphCommit {
        with (value) {
            return GraphCommit(
                commit = commitMapper.toDomain(value),
                lane = lane.position,
                passingLanes = passingLanes.map { it.position },
                mergingLanes = mergingLanes.map { it.position },
                forkingOffLanes = forkingOffLanes.map { it.position },
                childCount = childCount,
            )
        }
    }

}