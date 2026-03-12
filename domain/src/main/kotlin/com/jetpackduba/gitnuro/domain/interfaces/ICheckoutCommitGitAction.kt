package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface ICheckoutCommitGitAction {
    suspend operator fun invoke(git: Git, revCommit: RevCommit): Unit

    suspend operator fun invoke(git: Git, hash: String): Unit
}