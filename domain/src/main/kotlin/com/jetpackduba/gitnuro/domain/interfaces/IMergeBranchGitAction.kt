package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IMergeBranchGitAction {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
    suspend operator fun invoke(
        git: Git,
        branch: Branch,
        fastForward: Boolean,
        mergeAutoStash: Boolean,
    ): Boolean
}