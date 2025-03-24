package com.jetpackduba.gitnuro


sealed interface Result<out T, out E> {
    data class Ok<T>(val value: T) : Result<T, Nothing>
    data class Err<E>(val error: E) : Result<Nothing, E>
}