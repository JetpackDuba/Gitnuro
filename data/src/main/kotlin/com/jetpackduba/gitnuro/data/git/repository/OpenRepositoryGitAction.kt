package com.jetpackduba.gitnuro.data.git.repository

import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.common.systemSeparator
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
    override suspend operator fun invoke(directory: String): String? {
        val directory = File(directory)

        if (!directory.isDirectory) {
            printError(TAG, "Can't open git repo, specified path is not a directory")
            return null
        }

        val repository = if (directory.listFiles()?.any { it.name == ".git" && it.isFile } == true) {
            openSubmoduleRepository(directory)
        } else {
            openRepository(directory)
        }

        if (repository == null) {
            printError(TAG, "Can't open git repo, specified path is not a directory")
            return null
        }

        try {
            repository.workTree // test if repository is valid
            return repository.directory.absolutePath
        } catch (e: Exception) {
            printError(TAG, "Can't open git repo", e)
            return null
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