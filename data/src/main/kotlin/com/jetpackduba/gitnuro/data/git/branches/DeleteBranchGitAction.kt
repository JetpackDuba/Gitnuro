package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.domain.interfaces.IDeleteBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class DeleteBranchGitAction @Inject constructor() : IDeleteBranchGitAction {
    override suspend operator fun invoke(git: Git, branch: Branch): List<String> = withContext(Dispatchers.IO) {
        git
            .branchDelete()
            .setBranchNames(branch.name)
            .setForce(true) // TODO Should it be forced?
            .call()
    }
}