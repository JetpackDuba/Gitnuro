package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import javax.inject.Inject

class CheckoutCommitGitAction @Inject constructor(
    private val jgit: JGit,
) : ICheckoutCommitGitAction {
    override suspend operator fun invoke(repositoryPath: String, commit: Commit) =
        this@CheckoutCommitGitAction(repositoryPath, commit.hash)

    override suspend operator fun invoke(repositoryPath: String, hash: String) = jgit.provide(repositoryPath) { git ->
        git
            .checkout()
            .setName(hash)
            .call()

        Unit
    }
}
