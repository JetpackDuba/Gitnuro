package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IGetSubmodulesGitAction
import javax.inject.Inject

class GetSubmodulesGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetSubmodulesGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git
            .submoduleStatus()
            .call()
    }
}