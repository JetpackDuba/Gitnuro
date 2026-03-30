package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IAddRemoteGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        fetchUri: String,
    ): Either<Unit, GitError>
}