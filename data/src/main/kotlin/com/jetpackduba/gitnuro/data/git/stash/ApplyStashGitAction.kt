package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.domain.interfaces.IApplyStashGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class ApplyStashGitAction @Inject constructor() : IApplyStashGitAction {
    override suspend operator fun invoke(git: Git, stashInfo: RevCommit): Unit = withContext(Dispatchers.IO) {
        git.stashApply()
            .setStashRef(stashInfo.name)
            .call()
    }
}