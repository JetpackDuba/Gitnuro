package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git

interface IDeleteFileGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String): Either<Unit, GitError>
}