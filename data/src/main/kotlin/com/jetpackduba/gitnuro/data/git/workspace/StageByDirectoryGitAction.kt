package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IStageByDirectoryGitAction
import javax.inject.Inject

class StageByDirectoryGitAction @Inject constructor(
    private val jgit: JGit,
) : IStageByDirectoryGitAction {
    override suspend operator fun invoke(repositoryPath: String, dir: String) = jgit.provide(repositoryPath) { git ->
        git
            .add()
            .addFilepattern(dir)
            .call()

        Unit
    }
}
