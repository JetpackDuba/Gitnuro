package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch

interface ISetTrackingBranchGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        branch: Branch,
        remoteName: String?,
        remoteBranch: Branch?
    ): Either<Unit, GitError>

    suspend operator fun invoke(
        repositoryPath: String,
        refName: String,
        remoteName: String?,
        remoteBranchName: String?
    ): Either<Unit, GitError>
}