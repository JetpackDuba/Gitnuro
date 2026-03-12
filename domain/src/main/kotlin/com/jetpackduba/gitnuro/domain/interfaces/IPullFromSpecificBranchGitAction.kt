package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IPullFromSpecificBranchGitAction {
    suspend operator fun invoke(git: Git, remoteBranch: Ref, pullWithRebase: Boolean): PullHasConflicts
}