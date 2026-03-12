package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IPushBranchGitAction {
    suspend operator fun invoke(git: Git, force: Boolean, pushTags: Boolean, pushWithLease: Boolean)
}