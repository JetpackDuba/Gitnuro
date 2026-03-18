package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface IApplyStashGitAction {
    suspend operator fun invoke(git: Git, stashInfo: Commit): Unit
}