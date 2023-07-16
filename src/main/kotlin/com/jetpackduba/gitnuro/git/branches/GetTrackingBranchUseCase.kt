package com.jetpackduba.gitnuro.git.branches

import com.jetpackduba.gitnuro.extensions.simpleName
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class GetTrackingBranchUseCase @Inject constructor() {
    operator fun invoke(git: Git, ref: Ref): TrackingBranch? {
        return this.invoke(git, ref.simpleName)
    }

    operator fun invoke(git: Git, refName: String): TrackingBranch? {
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

data class TrackingBranch(val remote: String, val branch: String)