package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.JGitTagMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetTagsGitAction
import javax.inject.Inject

class GetTagsGitAction @Inject constructor(
    private val tagMapper: JGitTagMapper,
    private val jgit: JGit,
) : IGetTagsGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git
            .tagList()
            .call()
            .mapNotNull { tagMapper.toDomain(it) }
    }
}