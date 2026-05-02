package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface ISyncSubmoduleGitAction {
    suspend operator fun invoke(repositoryPath: String, path: String): Either<Unit, GitError>
}