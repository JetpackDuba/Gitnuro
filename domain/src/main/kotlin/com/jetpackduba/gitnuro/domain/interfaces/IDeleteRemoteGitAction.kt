package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IDeleteRemoteGitAction {
    suspend operator fun invoke(repositoryPath: String, remoteName: String): Either<Unit, GitError>
}