package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.credentials.CredentialsHandler
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IHandleTransportGitAction {
    suspend operator fun <R> invoke(repositoryPath: String?, block: suspend CredentialsHandler.() -> R): Either<R, GitError>
}