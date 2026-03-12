package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.PullType
import org.eclipse.jgit.api.Git

interface IPullBranchGitAction {
    suspend operator fun invoke(
        git: Git,
        pullType: PullType,
        mergeAutoStash: Boolean = true, // TODO Fix this after refactor
    ): PullHasConflicts
}