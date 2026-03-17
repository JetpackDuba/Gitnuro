package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.domain.interfaces.ISetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.StoredConfig
import javax.inject.Inject

class SetTrackingBranchGitAction @Inject constructor() : ISetTrackingBranchGitAction {
    override operator fun invoke(git: Git, branch: Branch, remoteName: String?, remoteBranch: Branch?) {
        invoke(git, branch.simpleName, remoteName, remoteBranch?.simpleName)
    }

    override operator fun invoke(git: Git, refName: String, remoteName: String?, remoteBranchName: String?) {
        val repository: Repository = git.repository
        val config: StoredConfig = repository.config

        if (remoteName == null || remoteBranchName == null) {
            config.unset("branch", refName, "remote")
            config.unset("branch", refName, "merge")
        } else {
            config.setString("branch", refName, "remote", remoteName)
            config.setString(
                "branch",
                refName,
                "merge",
                BranchesConstants.UPSTREAM_BRANCH_CONFIG_PREFIX + remoteBranchName
            )
        }

        config.save()
    }
}
