package app.extensions

import org.eclipse.jgit.lib.RepositoryState

val RepositoryState.isMerging
    get() = this == RepositoryState.MERGING || this == RepositoryState.MERGING_RESOLVED

val RepositoryState.isCherryPicking
    get() = this == RepositoryState.CHERRY_PICKING || this == RepositoryState.CHERRY_PICKING_RESOLVED

val RepositoryState.isReverting
    get() = this == RepositoryState.REVERTING || this == RepositoryState.REVERTING_RESOLVED