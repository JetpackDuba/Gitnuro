package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteSubmoduleGitAction
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

private const val TAG = "DeleteSubmoduleGitAction"

class DeleteSubmoduleGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteSubmoduleGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        path: String,
    ) = jgit.provide(repositoryPath) { git ->
        git.rm().addFilepattern(path).call()

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
