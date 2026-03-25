package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.DiffResult
import com.jetpackduba.gitnuro.domain.models.DiffType
import org.eclipse.jgit.api.Git

interface IFormatDiffGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        diffType: DiffType,
        isDisplayFullFile: Boolean,
    ): Either<DiffResult, GitError>
}