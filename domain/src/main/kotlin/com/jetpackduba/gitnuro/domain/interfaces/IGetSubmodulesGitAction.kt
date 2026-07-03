package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Submodule

interface IGetSubmodulesGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<Map<String, Submodule>, GitError>
}