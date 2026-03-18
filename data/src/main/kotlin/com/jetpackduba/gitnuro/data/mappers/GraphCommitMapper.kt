package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.extensions.isBranch
import com.jetpackduba.gitnuro.domain.extensions.isTag
import com.jetpackduba.gitnuro.domain.git.graph.GraphNode
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import com.jetpackduba.gitnuro.domain.models.Tag
import javax.inject.Inject

class GraphCommitMapper @Inject constructor(
    private val branchMapper: JGitBranchMapper,
    private val tagMapper: JGitTagMapper,
    private val commitMapper: JGitCommitMapper,
): DataMapper<GraphCommit, GraphNode> {
    override fun toData(value: GraphCommit): Nothing {
        TODO("Not yet implemented")
    }

    override fun toDomain(value: GraphNode): GraphCommit {
        with (value) {
            val branches = mutableListOf<Branch>()
            val tags = mutableListOf<Tag>()

            for (ref in refs) {
                if (ref.isBranch) {
                    branchMapper
                        .toDomain(ref)
                        ?.let { branches.add(it) }
                } else if (ref.isTag) {
                    tagMapper
                        .toDomain(ref)
                        ?.let { tags.add(it) }
                }
            }

            return GraphCommit(
                commit = commitMapper.toDomain(value),
                lane = lane.position,
                passingLanes = passingLanes.map { it.position },
                mergingLanes = mergingLanes.map { it.position },
                forkingOffLanes = forkingOffLanes.map { it.position },
                isStash = value.isStash,
                childCount = childCount,
                branches = branches,
                tags = tags,
            )
        }
    }

}