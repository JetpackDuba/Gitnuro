package app.extensions

import org.eclipse.jgit.lib.RepositoryState

val RepositoryState.isMerging
    get() = this == RepositoryState.MERGING || this == RepositoryState.MERGING_RESOLVED