package com.jetpackduba.gitnuro.domain.git.branches

import com.jetpackduba.gitnuro.common.extensions.runIf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CreateBranchGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git, branchName: String, targetCommit: RevCommit?): Ref =
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