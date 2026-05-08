package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.common.extensions.runIfNotNull
import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.ICreateBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import javax.inject.Inject

class CreateBranchGitAction @Inject constructor(
    private val jgit: JGit,
) : ICreateBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, branchName: String, targetCommit: Commit?) =
        jgit.provide(repositoryPath) { git ->
            git
                .checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .runIfNotNull(targetCommit) { commit ->
                    setStartPoint(commit.hash)
                }
                .call()

            Unit
        }
}