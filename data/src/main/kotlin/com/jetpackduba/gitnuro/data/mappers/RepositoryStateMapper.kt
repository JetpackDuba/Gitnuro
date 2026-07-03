package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.models.RepositoryState
import org.eclipse.jgit.lib.RepositoryState as JGitRepositoryState
import javax.inject.Inject

class RepositoryStateMapper @Inject constructor() : DataMapper<RepositoryState, JGitRepositoryState> {
    override fun toData(value: RepositoryState): Nothing {
        throw NotImplementedError("Mapping of RepositoryState domain to data not implemented")
    }

    override fun toDomain(value: JGitRepositoryState): RepositoryState {
        return when (value) {
            JGitRepositoryState.BARE -> RepositoryState.BARE
            JGitRepositoryState.SAFE -> RepositoryState.SAFE
            JGitRepositoryState.MERGING -> RepositoryState.MERGING
            JGitRepositoryState.MERGING_RESOLVED -> RepositoryState.MERGING_RESOLVED
            JGitRepositoryState.CHERRY_PICKING -> RepositoryState.CHERRY_PICKING
            JGitRepositoryState.CHERRY_PICKING_RESOLVED -> RepositoryState.CHERRY_PICKING_RESOLVED
            JGitRepositoryState.REVERTING -> RepositoryState.REVERTING
            JGitRepositoryState.REVERTING_RESOLVED -> RepositoryState.REVERTING_RESOLVED
            JGitRepositoryState.REBASING -> RepositoryState.REBASING
            JGitRepositoryState.REBASING_REBASING -> RepositoryState.REBASING_REBASING
            JGitRepositoryState.APPLY -> RepositoryState.APPLY
            JGitRepositoryState.REBASING_MERGE -> RepositoryState.REBASING_MERGE
            JGitRepositoryState.REBASING_INTERACTIVE -> RepositoryState.REBASING_INTERACTIVE
            JGitRepositoryState.BISECTING -> RepositoryState.BISECTING
        }
    }
}