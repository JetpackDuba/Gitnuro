package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IPushToSpecificBranchGitAction {
    suspend operator fun invoke(git: Git, force: Boolean, pushTags: Boolean, remoteBranch: Branch)
}