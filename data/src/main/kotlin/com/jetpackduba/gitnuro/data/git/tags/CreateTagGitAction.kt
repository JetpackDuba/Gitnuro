package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.domain.interfaces.ICreateTagGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CreateTagGitAction @Inject constructor() : ICreateTagGitAction {
    override suspend operator fun invoke(git: Git, tag: String, revCommit: RevCommit): Unit = withContext(Dispatchers.IO) {
        git
            .tag()
            .setAnnotated(true)
            .setName(tag)
            .setObjectId(revCommit)
            .call()
    }
}