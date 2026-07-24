package com.jetpackduba.gitnuro.ui

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.GenericError

@Composable
fun AppError.getErrorText(): String {
    return if (this is GenericError) {
        return this.message
    } else {
        // TODO Add proper error message for every error type
        this.toString()
    }
}