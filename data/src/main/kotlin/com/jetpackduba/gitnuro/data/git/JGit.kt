package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GenericError
import com.jetpackduba.gitnuro.domain.errors.GitError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File

// TODO instead of this, consider creating a pool of (J)Git instances that can be used by different actions of
//  different tabs or just some state holder with it
suspend fun <T> jgit(
    path: String,
    errorHandle: ((Exception) -> GitError)? = null,
    block: suspend Git.() -> T,
): Either<T, GitError> {
    return withContext(Dispatchers.IO) {
        try {
            val result = Git
                .open(File(path))
                .block()

            Either.Ok(result)
        } catch (ex: Exception) {
            val error = errorHandle?.invoke(ex) ?: GenericError(ex.message.orEmpty())
            Either.Err(error)
        }
    }
}
