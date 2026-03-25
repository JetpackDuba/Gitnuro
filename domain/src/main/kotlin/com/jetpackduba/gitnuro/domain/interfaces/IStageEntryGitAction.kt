package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.StatusEntry

interface IStageEntryGitAction {
    suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry): Either<Unit, AppError>
}