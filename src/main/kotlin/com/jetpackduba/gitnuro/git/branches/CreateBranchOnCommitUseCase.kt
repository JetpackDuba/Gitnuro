package com.jetpackduba.gitnuro.git.branches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CreateBranchOnCommitUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, branch: String, revCommit: RevCommit): Ref = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setCreateBranch(true)
            .setName(branch)
            .setStartPoint(revCommit)
            .call()
    }
}