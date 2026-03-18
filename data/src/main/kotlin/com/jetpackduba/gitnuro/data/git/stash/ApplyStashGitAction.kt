package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.domain.interfaces.IApplyStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class ApplyStashGitAction @Inject constructor() : IApplyStashGitAction {
    override suspend operator fun invoke(git: Git, stashInfo: Commit): Unit = withContext(Dispatchers.IO) {
        git.stashApply()
            .setStashRef(stashInfo.hash)
            .call()
    }
}