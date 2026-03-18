package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.domain.interfaces.ICherryPickCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CherryPickResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CherryPickCommitGitAction @Inject constructor() : ICherryPickCommitGitAction {
    override suspend operator fun invoke(git: Git, revCommit: Commit): CherryPickResult = withContext(Dispatchers.IO) {
        val base =
            git.repository.resolve(revCommit.hash) ?: throw Exception("Commit ${revCommit.hash} not found")

        git.cherryPick()
            .include(base)
            .call()
    }
}