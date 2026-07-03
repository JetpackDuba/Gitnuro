package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.JGitSubmoduleMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetSubmodulesGitAction
import javax.inject.Inject

class GetSubmodulesGitAction @Inject constructor(
    private val jgit: JGit,
    private val submoduleMapper: JGitSubmoduleMapper,
) : IGetSubmodulesGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val submodules = git
            .submoduleStatus()
            .call()

        submodules
            .mapValues { submodule ->
                submoduleMapper.toDomain(submodule.value)
            }
            .toMap()
    }
}