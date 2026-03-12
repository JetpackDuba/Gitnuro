package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface IApplyStashGitAction {
    suspend operator fun invoke(git: Git, stashInfo: RevCommit): Unit
}