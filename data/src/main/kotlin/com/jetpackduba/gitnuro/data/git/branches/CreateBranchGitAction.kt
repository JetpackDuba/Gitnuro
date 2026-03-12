package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.common.extensions.runIf
import com.jetpackduba.gitnuro.domain.interfaces.ICreateBranchGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CreateBranchGitAction @Inject constructor() : ICreateBranchGitAction {
    override suspend operator fun invoke(git: Git, branchName: String, targetCommit: RevCommit?): Ref =
        withContext(Dispatchers.IO) {
            git
                .checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .runIf(targetCommit != null) {
                    setStartPoint(targetCommit)
                }
                .call()
        }
}