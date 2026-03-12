package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit

interface IFindCommitGitAction {
    suspend operator fun invoke(git: Git, commitId: String): RevCommit?

    suspend operator fun invoke(git: Git, commitId: ObjectId): RevCommit?
}