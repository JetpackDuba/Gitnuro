package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.SignOffConfig

interface ISaveLocalRepositoryConfigGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        signOffConfig: SignOffConfig,
    ): Either<Unit, GitError>
}