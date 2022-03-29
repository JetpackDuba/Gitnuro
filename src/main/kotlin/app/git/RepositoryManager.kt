package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import javax.inject.Inject

class RepositoryManager @Inject constructor() {
    suspend fun getRepositoryState(git: Git) = withContext(Dispatchers.IO) {
        return@withContext git.repository.repositoryState
    }

    fun openRepository(directory: File): Repository {
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
        return builder.setGitDir(gitDirectory)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build()
    }
}