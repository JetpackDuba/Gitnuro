package com.jetpackduba.gitnuro.git.branches

import com.jetpackduba.gitnuro.extensions.simpleName
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.StoredConfig
import javax.inject.Inject

class SetTrackingBranchUseCase @Inject constructor() {
    operator fun invoke(git: Git, ref: Ref, remoteName: String?, remoteBranch: Ref?) {
        invoke(git, ref.simpleName, remoteName, remoteBranch?.simpleName)
    }

    operator fun invoke(git: Git, refName: String, remoteName: String?, remoteBranchName: String?) {
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
