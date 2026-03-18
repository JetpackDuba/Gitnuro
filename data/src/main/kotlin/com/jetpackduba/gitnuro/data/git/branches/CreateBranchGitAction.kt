package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.common.extensions.runIfNotNull
import com.jetpackduba.gitnuro.domain.interfaces.ICreateBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class CreateBranchGitAction @Inject constructor() : ICreateBranchGitAction {
    override suspend operator fun invoke(git: Git, branchName: String, targetCommit: Commit?): Ref =
        withContext(Dispatchers.IO) {
            git
                .checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .runIfNotNull(targetCommit) { commit ->
                    setStartPoint(commit.hash)
                }
                .call()
        }
}