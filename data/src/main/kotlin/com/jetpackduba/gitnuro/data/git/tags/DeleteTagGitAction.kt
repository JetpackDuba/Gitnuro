package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.domain.interfaces.IDeleteTagGitAction
import com.jetpackduba.gitnuro.domain.models.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class DeleteTagGitAction @Inject constructor() : IDeleteTagGitAction {
    override suspend operator fun invoke(git: Git, tag: Tag): Unit = withContext(Dispatchers.IO) {
        git
            .tagDelete()
            .setTags(tag.hash)
            .call()
    }
}