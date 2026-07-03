package com.jetpackduba.gitnuro.domain.models

enum class RepositoryState {
    BARE,
    SAFE,
    MERGING,
    MERGING_RESOLVED,
    CHERRY_PICKING,
    CHERRY_PICKING_RESOLVED,
    REVERTING,
    REVERTING_RESOLVED,
    REBASING,
    REBASING_REBASING,
    APPLY,
    REBASING_MERGE,
    REBASING_INTERACTIVE,
    BISECTING;

    val isMerging
        get() = this == RepositoryState.MERGING || this == RepositoryState.MERGING_RESOLVED

    val isRebasing
        get() = this == RepositoryState.REBASING || this == RepositoryState.REBASING_REBASING ||
                this == RepositoryState.REBASING_MERGE || this == RepositoryState.REBASING_INTERACTIVE

    val isCherryPicking
        get() = this == RepositoryState.CHERRY_PICKING || this == RepositoryState.CHERRY_PICKING_RESOLVED

    val isReverting
        get() = this == RepositoryState.REVERTING || this == RepositoryState.REVERTING_RESOLVED
}