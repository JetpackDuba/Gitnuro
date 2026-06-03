package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.BranchesConstants
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IGetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TrackingBranch
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class GetTrackingBranchGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetTrackingBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, branch: Branch): Either<TrackingBranch?, GitError> {
        return this.invoke(repositoryPath, branch.simpleName)
    }

    override suspend operator fun invoke(repositoryPath: String, refName: String) =
        jgit.provide(repositoryPath) { git ->
            val repository: Repository = git.repository

            val config: Config = repository.config
            val remote: String? = config.getString("branch", refName, "remote")
            val branch: String? = config.getString("branch", refName, "merge")

            if (remote != null && branch != null) {
                TrackingBranch(remote, branch.removePrefix(BranchesConstants.UPSTREAM_BRANCH_CONFIG_PREFIX))
            } else {
                null
            }
        }
}

