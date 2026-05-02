package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import org.eclipse.jgit.api.Git

interface ISaveAuthorGitAction {
    suspend operator fun invoke(repositoryPath: String, newAuthorInfo: AuthorInfo): Either<Unit, GitError>
}