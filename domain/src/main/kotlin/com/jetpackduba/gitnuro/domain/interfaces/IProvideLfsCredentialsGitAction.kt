package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.common.Either
import com.jetpackduba.gitnuro.domain.errors.LfsError

interface IProvideLfsCredentialsGitAction {
    suspend operator fun <T> invoke(
        url: String,
        callback: suspend (username: String?, password: String?) -> Either<T, LfsError>,
    ): Either<T, LfsError>
}