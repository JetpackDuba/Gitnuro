package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch

interface IPushToSpecificBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, force: Boolean, pushTags: Boolean, remoteBranch: Branch): Either<Unit, GitError>
}