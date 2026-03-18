package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.data.mappers.JGitTagMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetTagsGitAction
import com.jetpackduba.gitnuro.domain.models.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetTagsGitAction @Inject constructor(
    private val tagMapper: JGitTagMapper,
) : IGetTagsGitAction {
    override suspend operator fun invoke(git: Git): List<Tag> = withContext(Dispatchers.IO) {
        return@withContext git
            .tagList()
            .call()
            .mapNotNull { tagMapper.toDomain(it) }
    }
}