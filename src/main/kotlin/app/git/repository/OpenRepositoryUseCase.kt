package app.git.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import javax.inject.Inject

class OpenRepositoryUseCase @Inject constructor() {
    suspend operator fun invoke(directory: File): Repository = withContext(Dispatchers.IO) {
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
}