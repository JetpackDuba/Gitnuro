package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.domain.BranchesConstants
import com.jetpackduba.gitnuro.domain.interfaces.IGetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TrackingBranch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class GetTrackingBranchGitAction @Inject constructor() : IGetTrackingBranchGitAction {
    override operator fun invoke(git: Git, branch: Branch): TrackingBranch? {
        return this.invoke(git, branch.simpleName)
    }

    override operator fun invoke(git: Git, refName: String): TrackingBranch? {
        val repository: Repository = git.repository

        val config: Config = repository.config
        val remote: String? = config.getString("branch", refName, "remote")
        val branch: String? = config.getString("branch", refName, "merge")

        if (remote != null && branch != null) {
            return TrackingBranch(remote, branch.removePrefix(BranchesConstants.UPSTREAM_BRANCH_CONFIG_PREFIX))
        }

        return null
    }
}

