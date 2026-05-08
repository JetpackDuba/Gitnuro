package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.ICherryPickCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import javax.inject.Inject

class CherryPickCommitGitAction @Inject constructor(
    private val jgit: JGit,
) : ICherryPickCommitGitAction {
    override suspend operator fun invoke(repositoryPath: String, commit: Commit) = jgit.provide(repositoryPath) { git ->
        val base =
            git.repository.resolve(commit.hash) ?: throw Exception("Commit ${commit.hash} not found")

        git.cherryPick()
            .include(base)
            .call()

        Unit
    }
}