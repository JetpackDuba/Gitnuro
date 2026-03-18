package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.data.JGitBranchMapper
import com.jetpackduba.gitnuro.domain.extensions.isBranch
import com.jetpackduba.gitnuro.domain.git.graph.GraphNode
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import javax.inject.Inject

class GraphCommitMapper @Inject constructor(
    private val identityMapper: JGitIdentityMapper,
    private val branchMapper: JGitBranchMapper,
    private val commitMapper: JGitCommitMapper,
): DataMapper<GraphCommit, GraphNode> {
    override fun toData(value: GraphCommit): Nothing {
        TODO("Not yet implemented")
    }

    override fun toDomain(value: GraphNode): GraphCommit {
        with (value) {
            return GraphCommit(
                lane = lane.position,
                commit = commitMapper.toDomain(value),
                branches = refs.mapNotNull {
                    if (it.isBranch) {
                        branchMapper.toDomain(it)
                    } else {
                        null
                    }
                },
                tags = emptyList(), // TODO Fill this once tags mapper is implemented
            )
        }
    }

}