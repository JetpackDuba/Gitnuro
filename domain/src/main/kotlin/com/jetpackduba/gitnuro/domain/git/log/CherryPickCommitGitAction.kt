package com.jetpackduba.gitnuro.domain.git.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CherryPickResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CherryPickCommitGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git, revCommit: RevCommit): CherryPickResult = withContext(Dispatchers.IO) {
        git.cherryPick()
            .include(revCommit)
            .call()
    }
}