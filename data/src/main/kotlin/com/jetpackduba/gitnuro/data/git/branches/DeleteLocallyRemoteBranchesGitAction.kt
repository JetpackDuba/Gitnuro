package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteLocallyRemoteBranchesGitAction
import javax.inject.Inject

class DeleteLocallyRemoteBranchesGitAction @Inject constructor(private val jgit: JGit) :
    IDeleteLocallyRemoteBranchesGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        branches: List<String>
    ) = jgit.provide(repositoryPath) { git ->
        git
            .branchDelete()
            .setBranchNames(*branches.toTypedArray())
            .setForce(true)
            .call()
    }
}