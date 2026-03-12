package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.CherryPickResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface ICherryPickCommitGitAction {
    suspend operator fun invoke(git: Git, revCommit: RevCommit): CherryPickResult
}