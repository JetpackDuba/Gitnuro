package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IStashChangesGitAction {
    suspend operator fun invoke(repositoryPath: String, message: String?): Either<Unit, GitError>
}