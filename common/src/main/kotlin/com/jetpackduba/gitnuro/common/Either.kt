package com.jetpackduba.gitnuro.common

sealed interface Either<out T, out E> {
    data class Ok<T>(val value: T) : Either<T, Nothing>
    data class Err<E>(val error: E) : Either<Nothing, E>
}