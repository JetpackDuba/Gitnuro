package com.jetpackduba.gitnuro.domain.git.tags

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CreateTagGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git, tag: String, revCommit: RevCommit): Unit = withContext(Dispatchers.IO) {
        git
            .tag()
            .setAnnotated(true)
            .setName(tag)
            .setObjectId(revCommit)
            .call()
    }
}