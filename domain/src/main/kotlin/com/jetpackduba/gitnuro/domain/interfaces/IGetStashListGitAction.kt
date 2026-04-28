package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Commit

interface IGetStashListGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<List<Commit>, GitError>
}