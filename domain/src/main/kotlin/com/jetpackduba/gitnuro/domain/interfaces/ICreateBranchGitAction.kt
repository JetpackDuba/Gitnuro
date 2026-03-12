package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

interface ICreateBranchGitAction {
    suspend operator fun invoke(git: Git, branchName: String, targetCommit: RevCommit?): Ref
}