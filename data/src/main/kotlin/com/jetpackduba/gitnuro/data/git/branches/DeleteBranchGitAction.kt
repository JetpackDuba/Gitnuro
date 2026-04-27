package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import javax.inject.Inject

class DeleteBranchGitAction @Inject constructor() : IDeleteBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, branch: Branch) = jgit(repositoryPath) {
        branchDelete()
            .setBranchNames(branch.name)
            .setForce(true) // TODO Should it be forced?
            .call()

        Unit
    }
}