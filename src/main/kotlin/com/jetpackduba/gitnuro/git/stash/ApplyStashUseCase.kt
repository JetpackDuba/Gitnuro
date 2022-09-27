package com.jetpackduba.gitnuro.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class ApplyStashUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, stashInfo: RevCommit): Unit = withContext(Dispatchers.IO) {
        git.stashApply()
            .setStashRef(stashInfo.name)
            .call()
    }
}