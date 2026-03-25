package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.Status

interface IGetStatusGitAction {
    suspend operator fun invoke(repository: String): Either<Status, AppError>
}