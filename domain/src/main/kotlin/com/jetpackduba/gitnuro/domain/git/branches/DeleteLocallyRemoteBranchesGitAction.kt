package com.jetpackduba.gitnuro.domain.git.branches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class DeleteLocallyRemoteBranchesGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git, branches: List<String>): List<String> = withContext(Dispatchers.IO) {
        git
            .branchDelete()
            .setBranchNames(*branches.toTypedArray())
            .setForce(true)
            .call()
    }
}