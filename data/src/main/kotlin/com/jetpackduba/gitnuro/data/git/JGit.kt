package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.domain.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGit @Inject constructor() {
    private val repositories = mutableMapOf<String, Git>()

    suspend fun <T> provide(
        repositoryPath: String,
        errorHandle: ((Exception) -> GitError)? = null,
        block: suspend EitherContext<GitError>.(Git) -> T,
    ) = either<T, GitError> {
        val cachedGit = repositories[repositoryPath]

        val git = if (cachedGit == null) {
            val newGit = handleException(
                exceptionMapper = { RepositoryReadError }
            ) {
                Git
                    .open(File(repositoryPath))
            }.bind()

            repositories[repositoryPath] = newGit
            newGit
        } else {
            cachedGit
        }

        try {
            Either.Ok(block(git))
        } catch (ex: Exception) {
            val error = errorHandle?.invoke(ex) ?: GenericError(ex.message.orEmpty())
            Either.Err(error)
        }
    }

    suspend fun <T> provideOptional(
        repositoryPath: String?,
        errorHandle: ((Exception) -> GitError)? = null,
        block: suspend EitherContext<GitError>.(Git?) -> T,
    ) = either<T, GitError> {

        val git = if (repositoryPath != null) {
            val cachedGit = repositories[repositoryPath]

            if (cachedGit == null) {
                val newGit = handleException(
                    exceptionMapper = { RepositoryReadError }
                ) {
                    Git
                        .open(File(repositoryPath))
                }.bind()

                repositories[repositoryPath] = newGit
                newGit
            } else {
                cachedGit
            }
        } else {
            null
        }

        try {
            Either.Ok(block(git))
        } catch (ex: Exception) {
            val error = errorHandle?.invoke(ex) ?: GenericError(ex.message.orEmpty())
            Either.Err(error)
        }
    }

    fun cleanup(repositoryPath: String) {
        repositories.remove(repositoryPath)
    }
}
