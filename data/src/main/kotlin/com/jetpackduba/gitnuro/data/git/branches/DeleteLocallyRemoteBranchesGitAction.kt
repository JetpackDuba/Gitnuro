package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.domain.interfaces.IDeleteLocallyRemoteBranchesGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class DeleteLocallyRemoteBranchesGitAction @Inject constructor() : IDeleteLocallyRemoteBranchesGitAction {
    override suspend operator fun invoke(git: Git, branches: List<String>): List<String> = withContext(Dispatchers.IO) {
        git
            .branchDelete()
            .setBranchNames(*branches.toTypedArray())
            .setForce(true)
            .call()
    }
}