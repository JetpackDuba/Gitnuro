package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import org.eclipse.jgit.blame.BlameResult

interface IBlameFileGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String): Either<BlameResult, GitError>
}