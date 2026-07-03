package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.RepositoryState

interface IGetRepositoryStateGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<RepositoryState, GitError>
}