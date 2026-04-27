package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IAddSubmoduleGitAction {
    suspend operator fun invoke(repositoryPath: String, name: String, path: String, uri: String): Either<Unit, GitError>
}