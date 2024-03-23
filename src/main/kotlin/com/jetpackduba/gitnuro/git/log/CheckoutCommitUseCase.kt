package com.jetpackduba.gitnuro.git.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CheckoutCommitUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, revCommit: RevCommit): Unit = withContext(Dispatchers.IO) {
        this@CheckoutCommitUseCase(git, revCommit.name)
    }
    suspend operator fun invoke(git: Git, hash: String): Unit = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setName(hash)
            .call()
    }
}