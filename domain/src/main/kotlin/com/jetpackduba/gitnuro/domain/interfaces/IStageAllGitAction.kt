package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git

interface IStageAllGitAction {
    suspend operator fun invoke(repositoryPath: String, entries: List<StatusEntry>?): Either<Unit, AppError>
}