package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import org.eclipse.jgit.api.Git

interface ILoadAuthorGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<AuthorInfo, GitError>
}