package com.jetpackduba.gitnuro.git.repository

import com.jetpackduba.gitnuro.exceptions.InvalidDirectoryException
import com.jetpackduba.gitnuro.system.systemSeparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.submodule.SubmoduleWalk
import java.io.File
import javax.inject.Inject

class OpenSubmoduleRepositoryUseCase @Inject constructor(
    private val openRepositoryUseCase: OpenRepositoryUseCase
) {
    suspend operator fun invoke(directory: File): Repository = withContext(Dispatchers.IO) {
        val parent = getRepositoryParent(directory)
            ?: throw InvalidDirectoryException("Submodule's parent repository not found")

        val repository = openRepositoryUseCase(parent)

        val submoduleRelativePath =
            directory.absolutePath.removePrefix("${repository.directory.parent}$systemSeparator")

        return@withContext SubmoduleWalk.getSubmoduleRepository(repository, submoduleRelativePath)
            ?: throw InvalidDirectoryException("Invalid submodule directory. Check if the submodule has been initialized before trying to open it.")
    }

    private fun getRepositoryParent(directory: File?): File? {
        if (directory == null) return null

        if (directory.listFiles()?.any { it.name == ".git" && it.isDirectory } == true) {
            return directory
        }

        return getRepositoryParent(directory.parentFile)
    }
}