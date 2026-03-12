package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutCommitGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CheckoutCommitGitAction @Inject constructor() : ICheckoutCommitGitAction {
    override suspend operator fun invoke(git: Git, revCommit: RevCommit): Unit = withContext(Dispatchers.IO) {
        this@CheckoutCommitGitAction(git, revCommit.name)
    }

    override suspend operator fun invoke(git: Git, hash: String): Unit = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setName(hash)
            .call()
    }
}
