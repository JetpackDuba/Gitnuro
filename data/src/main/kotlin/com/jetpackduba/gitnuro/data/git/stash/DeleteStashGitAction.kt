package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class DeleteStashGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteStashGitAction {
    override suspend operator fun invoke(repositoryPath: String, stashInfo: Commit) =
        jgit.provide(repositoryPath) { git ->
            invoke(git, stashInfo)
        }

    operator fun invoke(git: Git, stashInfo: Commit) {
        val stashList = git
            .stashList()
            .call()
            .toList()

        val indexOfStashToDelete = stashList.indexOfFirst { it.name == stashInfo.hash }

        if (indexOfStashToDelete != -1) {
            git
                .stashDrop()
                .setStashRef(indexOfStashToDelete)
                .call()
        }
    }
}