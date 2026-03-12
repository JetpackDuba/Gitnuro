package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.domain.interfaces.IGetTagsGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetTagsGitAction @Inject constructor() : IGetTagsGitAction {
    override suspend operator fun invoke(git: Git): List<Ref> = withContext(Dispatchers.IO) {
        return@withContext git.tagList().call()
    }
}