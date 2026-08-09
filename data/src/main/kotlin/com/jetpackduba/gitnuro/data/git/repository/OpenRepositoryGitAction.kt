package com.jetpackduba.gitnuro.data.git.repository

import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.OpenRepoError
import com.jetpackduba.gitnuro.domain.exceptions.InvalidDirectoryException
import com.jetpackduba.gitnuro.domain.interfaces.IOpenRepositoryGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.submodule.SubmoduleWalk
import java.io.File
import javax.inject.Inject

private const val TAG = "OpenRepositoryGitAction"

class OpenRepositoryGitAction @Inject constructor() : IOpenRepositoryGitAction {
    override suspend operator fun invoke(directory: String): Either<String, AppError> {
        val directory = File(directory)

        if (!directory.exists()) {
            printError(TAG, "Can't open git repo, specified path does not exist")
            return Either.Err(OpenRepoError.DirectoryNotFoundError)
        }

        if (!directory.isDirectory) {
            printError(TAG, "Can't open git repo, specified path is not a directory")
            return Either.Err(OpenRepoError.PathIsNotDirectory)
        }

        val repository = if (directory.listFiles()?.any { it.name == ".git" && it.isFile } == true) {
            openSubmoduleRepository(directory)
        } else {
            openRepository(directory)
        }

        if (repository == null) {
            printError(TAG, "Can't open git repo, specified path is not a directory")
            return Either.Err(OpenRepoError.RepositoryNotFoundInPath)
        }

        try {
            repository.workTree // test if repository is valid
            return Either.Ok(repository.directory.absolutePath)
        } catch (e: Exception) {
            printError(TAG, "Can't open git repo", e)
            return Either.Err(OpenRepoError.RepositoryLoadFailed(e.message.orEmpty()))
        }
    }

    private suspend fun openRepository(directory: File): Repository = withContext(Dispatchers.IO) {
        val gitDirectory = if (directory.name == ".git") {
            directory
        } else {
            val gitDir = File(directory, ".git")
            if (gitDir.exists() && gitDir.isDirectory) {
                gitDir
            } else
                directory
        }

        val builder = FileRepositoryBuilder()
        return@withContext builder.setGitDir(gitDirectory)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build()
    }

    private suspend fun openSubmoduleRepository(directory: File): Repository? = withContext(Dispatchers.IO) {
        val parent = getRepositoryParent(directory)
            ?: throw InvalidDirectoryException("Submodule's parent repository not found")

        val repository = openRepository(parent)

        val submoduleRelativePath =
            directory.absolutePath.removePrefix("${repository.directory.parent}$systemSeparator")

        return@withContext SubmoduleWalk.getSubmoduleRepository(repository, submoduleRelativePath)
    }

    private fun getRepositoryParent(directory: File?): File? {
        if (directory == null) return null

        if (directory.listFiles()?.any { it.name == ".git" && it.isDirectory } == true) {
            return directory
        }

        return getRepositoryParent(directory.parentFile)
    }
}