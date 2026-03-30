package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteLocallyRemoteBranchesGitAction
import javax.inject.Inject

class DeleteLocallyRemoteBranchesGitAction @Inject constructor() : IDeleteLocallyRemoteBranchesGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        branches: List<String>
    ): Either<List<String>, GitError> {
        return jgit(repositoryPath) {
            branchDelete()
                .setBranchNames(*branches.toTypedArray())
                .setForce(true)
                .call()
        }
    }
}