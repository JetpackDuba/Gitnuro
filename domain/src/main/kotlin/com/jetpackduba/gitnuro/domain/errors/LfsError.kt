package com.jetpackduba.gitnuro.domain.errors

import io.ktor.http.HttpStatusCode

sealed interface LfsError {
    data class HttpError(val code: HttpStatusCode) : LfsError
}