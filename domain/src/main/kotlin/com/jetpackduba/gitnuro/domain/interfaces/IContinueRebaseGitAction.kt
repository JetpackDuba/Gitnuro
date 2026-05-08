package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IContinueRebaseGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<Unit, GitError>
}