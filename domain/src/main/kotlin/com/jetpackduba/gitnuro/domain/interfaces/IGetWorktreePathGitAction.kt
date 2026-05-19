package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IGetWorktreePathGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<String, GitError>
}