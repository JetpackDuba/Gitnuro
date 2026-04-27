package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch

interface IDeleteBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, branch: Branch): Either<Unit, GitError>
}