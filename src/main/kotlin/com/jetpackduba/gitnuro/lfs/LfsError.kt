package com.jetpackduba.gitnuro.lfs

import io.ktor.http.*

sealed interface LfsError {
    data class HttpError(val code: HttpStatusCode) : LfsError
}