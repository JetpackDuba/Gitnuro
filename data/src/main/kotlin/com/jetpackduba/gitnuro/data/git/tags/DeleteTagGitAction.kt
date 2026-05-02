package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteTagGitAction
import com.jetpackduba.gitnuro.domain.models.Tag
import javax.inject.Inject

class DeleteTagGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteTagGitAction {
    override suspend operator fun invoke(repositoryPath: String, tag: Tag) = jgit.provide(repositoryPath) { git ->
        git
            .tagDelete()
            .setTags(tag.hash)
            .call()

        Unit
    }
}