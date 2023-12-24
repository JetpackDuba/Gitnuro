package com.jetpackduba.gitnuro.git.branches

import com.jetpackduba.gitnuro.extensions.simpleName
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.StoredConfig
import javax.inject.Inject

class SetTrackingBranchUseCase @Inject constructor() {
    operator fun invoke(git: Git, ref: Ref, remoteName: String?, remoteBranch: Ref?) {
        val repository: Repository = git.repository
        val config: StoredConfig = repository.config

        if (remoteName == null || remoteBranch == null) {
            config.unset("branch", ref.simpleName, "remote")
            config.unset("branch", ref.simpleName, "merge")
        } else {
            config.setString("branch", ref.simpleName, "remote", remoteName)
            config.setString(
                "branch",
                ref.simpleName,
                "merge",
                BranchesConstants.UPSTREAM_BRANCH_CONFIG_PREFIX + remoteBranch.simpleName
            )
        }

        config.save()
    }
}
