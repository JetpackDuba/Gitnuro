package com.jetpackduba.gitnuro.git.submodules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

private const val TAG = "DeleteSubmoduleUseCase"

class DeleteSubmoduleUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, path: String): Unit = withContext(Dispatchers.IO) {
        git.rm()
            .addFilepattern(path)
            .call()

        val repository = git.repository
        val gitModules = File(repository.workTree, ".gitmodules")

        if (gitModules.exists() && gitModules.isFile) {
            val config = FileBasedConfig(gitModules, repository.fs)

            config.load()
            config.unsetSection("submodule", path)
            config.save()
        }

        val moduleDir = File(repository.directory, "modules/$path")
        val workspace = File(repository.workTree, path)

        if (moduleDir.exists()) {
            moduleDir.deleteRecursively()
        }

        if (workspace.exists()) {
            workspace.deleteRecursively()
        }
    }
}
