@file:OptIn(ExperimentalContracts::class)

package com.jetpackduba.gitnuro.domain.errors

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException


/**
 * Special exception type used to control the flow in [EitherContext].
 *
 * Do not use out of this file.
 */
@Suppress("ObjectInheritsException") // We don't want new stacktraces/instances to be created for each throw
object EitherThrowable : Throwable() {
    @Suppress("unused") // It's required for Throwable subclasses
    private fun readResolve(): Any = EitherThrowable
}

sealed interface Either<out T, out E> {
    data class Ok<T>(
        val value: T
    ) : Either<T, Nothing>

    data class Err<E>(
        val error: E,
    ) : Either<Nothing, E>
}

// [onOk] & [onErr] are extension methods instead of member methods because member methods
// of interfaces can't be inlined
inline fun <T, E> Either<T, E>.onErr(callback: (E) -> Unit): Either<T, E> {
    if (this is Either.Err<E>) {
        callback(this.error)
    }

    return this
}

inline fun <T, E> Either<T, E>.onOk(callback: (T) -> Unit): Either<T, E> {
    if (this is Either.Ok<T>) {
        callback(this.value)
    }

    return this
}

/**
 * If [this] is [Either.Err], the error will be mapped to another new value, if [Either.Ok], it will
 * remain unchanged.
 */
inline fun <T, E, F> Either<T, E>.mapErr(callback: (E) -> F): Either<T, F> {
    val either = when (this) {
        is Either.Err -> Either.Err(callback(this.error))
        is Either.Ok -> this
    }

    return either
}

/**
 * If [this] is [Either.Ok], the value will be mapped to another new value, if [Either.Err], it will
 * remain unchanged.
 */
inline fun <T, E, F> Either<T, E>.mapOk(callback: (T) -> F): Either<F, E> {
    val either = when (this) {
        is Either.Err -> this
        is Either.Ok -> Either.Ok(callback(this.value))
    }

    return either
}

/**
 * Unfolds the [Either] to return [Either.Ok.value] or null
 */
fun <T, E> Either<T, E>.okOrNull(): T? = when (this) {
    is Either.Err -> null
    is Either.Ok -> this.value
}

/**
 * Unfolds the [Either] to return [Either.Err.error] or null
 */
fun <T, E> Either<T, E>.errOrNull(): E? = when (this) {
    is Either.Err -> this.error
    is Either.Ok -> null
}

suspend inline fun <T, E> handleException(
    dispatcher: CoroutineDispatcher,
    crossinline exceptionMapper: suspend (Exception) -> E,
    crossinline callback: suspend EitherContext<E>.() -> T
): Either<T, E> = withContext(dispatcher) {
    handleException(exceptionMapper, callback)
}

suspend inline fun <T, E> handleException(
    crossinline exceptionMapper: suspend (Exception) -> E,
    crossinline callback: suspend EitherContext<E>.() -> T
): Either<T, E> {
    val context = EitherContext<E>()
    return try {
        val res = context.callback()

        Either.Ok(res)
    } catch (ex: Throwable) {
        if (ex is EitherThrowable) {
            Either.Err(checkNotNull(context.error))
        } else if (ex is Exception && ex !is CancellationException) {
            val error = exceptionMapper(ex)

            Either.Err(error)
        } else {
            throw ex
        }
    }
}

suspend inline fun <T, E> either(
    crossinline callback: suspend EitherContext<E>.() -> Either<T, E>,
): Either<T, E> {
    contract {
        callsInPlace(callback, InvocationKind.AT_MOST_ONCE)
    }

    val context = EitherContext<E>(Job())

    return try {
        val result = context.callback()
        result
    } catch (_: EitherThrowable) {
        Either.Err(checkNotNull(context.error))
    }
}

suspend inline fun <T, E> either(
    dispatcher: CoroutineDispatcher,
    crossinline callback: suspend EitherContext<E>.() -> Either<T, E>,
): Either<T, E> = withContext(dispatcher) {
    return@withContext either(callback)
}

class EitherContext<E>(override val coroutineContext: CoroutineContext) : CoroutineScope {
    var error: E? = null
}

/**
 * Returns the current function in an [EitherContext] with [error].
 */
fun <E> EitherContext<E>.raiseError(error: E): Nothing {
    this.error = error
    throw EitherThrowable
}

/**
 * Returns the current function in an [EitherContext] if [value] is null with the error provided in
 * [errorCallback].
 */
inline fun <T, E> EitherContext<E>.raiseErrorIfNull(value: T?, errorCallback: () -> E): T {
    contract {
        returns() implies (value != null)
    }

    if (value == null) {
        val error = errorCallback()
        raiseError(error)
    }

    return value
}

context(context: EitherContext<E>)
suspend inline fun <T, E> T?.raiseErrorIfNull(
    errorIfNull: () -> E,
    noinline logging: suspend (E) -> Unit,
): T {
    contract {
        callsInPlace(errorIfNull, InvocationKind.AT_MOST_ONCE)
        callsInPlace(logging, InvocationKind.AT_MOST_ONCE)
    }

    if (this == null) {
        val error = errorIfNull()
        logging.invoke(error)
        context.raiseError(error)
    } else {
        return this
    }
}

/**
 * Returns the current function in an [EitherContext] with the error value if [this] is [Either.Err]
 */
context(context: EitherContext<E>)
fun <T, E> Either<T, E>.bind(): T {
    return when (this) {
        is Either.Err -> context.raiseError(this.error)
        is Either.Ok -> this.value
    }
}

/**
 * Transforms a nullable type to an [Either]. If [this] is null, the type will be [Either.Err]
 * obtained from [errIfNull].
 *
 * @param errIfNull - Lambda with the error when the original value is null
 */
fun <T, E> T?.toEither(errIfNull: () -> E): Either<T, E> {
    return if (this == null) {
        Either.Err(errIfNull())
    } else {
        Either.Ok(this)
    }
}

