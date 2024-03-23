package com.jetpackduba.gitnuro.exceptions

import kotlinx.coroutines.CancellationException

/**
 * Used to transform generic exceptions that methods may throw (such as IOException) to more specific custom exceptions
 */
fun <T> mapException(transform: (Exception) -> Exception, block: () -> T) {
    try {
        block()
    } catch (ex: Exception) {
        if (ex is CancellationException) {
            throw ex // Coroutines cancellation must be passed up
        } else {
            val newException = transform(ex)

            throw newException
        }
    }
}