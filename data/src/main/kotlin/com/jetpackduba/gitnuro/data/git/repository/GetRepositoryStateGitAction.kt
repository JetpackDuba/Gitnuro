package com.jetpackduba.gitnuro.data.git.repository

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import javax.inject.Inject

class GetRepositoryStateGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetRepositoryStateGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git.repository.repositoryState
    }
}