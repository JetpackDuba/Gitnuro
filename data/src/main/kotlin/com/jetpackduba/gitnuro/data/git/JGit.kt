package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.domain.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.FS_Win32
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class JGit @Inject constructor(
    private val windowsFs: Provider<WindowsFs>,
) {
    private val repositories = mutableMapOf<String, Git>()

    suspend fun <T> provide(
        repositoryPath: String,
        errorHandle: ((Exception) -> GitError)? = null,
        block: suspend EitherContext<GitError>.(Git) -> T,
    ) = either<T, GitError> {
        val cachedGit = repositories[repositoryPath]

        val git = if (cachedGit == null) {
            val newGit = handleException(
                exceptionMapper = { RepositoryReadError(it.message.orEmpty()) }
            ) {
                val fs = FS.detect()

                val fsToUse = if (fs is FS_Win32) {
                    windowsFs.get()
                } else {
                    fs
                }

                Git
                    .open(File(repositoryPath), fsToUse)
            }.bind()

            repositories[repositoryPath] = newGit
            newGit
        } else {
            cachedGit
        }

        try {
            Either.Ok(block(git))
        } catch (ex: Exception) {
            val error = errorHandle?.invoke(ex) ?: GenericError(ex.message.orEmpty(), ex)
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
                    exceptionMapper = { RepositoryReadError(it.message.orEmpty()) }
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
            val error = errorHandle?.invoke(ex) ?: GenericError(ex.message.orEmpty(), ex)
            Either.Err(error)
        }
    }

    fun cleanup(repositoryPath: String) {
        repositories.remove(repositoryPath)
    }
}

