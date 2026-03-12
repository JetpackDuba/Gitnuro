package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IMergeBranchGitAction {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
    suspend operator fun invoke(
        git: Git,
        branch: Ref,
        fastForward: Boolean,
        mergeAutoStash: Boolean,
    ): Boolean
}