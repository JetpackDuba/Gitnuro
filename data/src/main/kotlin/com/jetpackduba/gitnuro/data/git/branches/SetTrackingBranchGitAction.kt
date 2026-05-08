package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.BranchesConstants
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.ISetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.StoredConfig
import javax.inject.Inject

class SetTrackingBranchGitAction @Inject constructor(
    private val jgit: JGit,
) : ISetTrackingBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, branch: Branch, remoteName: String?, remoteBranch: Branch?): Either<Unit, GitError> {
        return invoke(repositoryPath, branch.simpleName, remoteName, remoteBranch?.simpleName)
    }

    override suspend operator fun invoke(repositoryPath: String, refName: String, remoteName: String?, remoteBranchName: String?) = jgit.provide(repositoryPath) { git ->
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
