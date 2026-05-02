package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.ISyncSubmoduleGitAction
import javax.inject.Inject

class SyncSubmoduleGitAction @Inject constructor(
    private val jgit: JGit,
) : ISyncSubmoduleGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        path: String,
    ) = jgit.provide(repositoryPath) { git ->
        git.submoduleSync()
            .addPath(path)
            .call()

        Unit
    }
}