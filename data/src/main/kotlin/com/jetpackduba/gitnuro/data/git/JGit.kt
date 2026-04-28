package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.domain.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File

// TODO instead of this, consider creating a pool of (J)Git instances that can be used by different actions of
//  different tabs or just some state holder with it
suspend fun <T> jgit(
    path: String,
    errorHandle: ((Exception) -> GitError)? = null,
    block: suspend Git.(Git) -> T,
): Either<T, GitError> {
    return withContext(Dispatchers.IO) {
        try {
            val result = Git
                .open(File(path))
                .run {
                    block(this)
                }


            Either.Ok(result)
        } catch (ex: Exception) {
            val error = errorHandle?.invoke(ex) ?: GenericError(ex.message.orEmpty())
            Either.Err(error)
        }
    }
}

suspend fun <T> jgit2(
    path: String,
    errorHandle: ((Exception) -> GitError)? = null,
    block: suspend EitherContext<GitError>.(Git) -> T,
): Either<T, GitError> {
    return either(Dispatchers.IO) {
        try {
            val result = Git
                .open(File(path))

            Either.Ok(this.block(result))
        } catch (ex: Exception) {
            val error = errorHandle?.invoke(ex) ?: GenericError(ex.message.orEmpty())
            Either.Err(error)
        }
    }
}
