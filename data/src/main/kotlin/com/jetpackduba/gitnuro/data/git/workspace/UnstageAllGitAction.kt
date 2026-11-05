package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import javax.inject.Inject

class UnstageAllGitAction @Inject constructor(
    private val jgit: JGit,
) : IUnstageAllGitAction {
    override suspend operator fun invoke(repositoryPath: String, entries: List<StatusEntry>?) =
        jgit.provide(repositoryPath) { git ->
            val resetCommand = git.reset()

            for (entry in entries.orEmpty()) {
                resetCommand.addPath(entry.filePath)
            }

            resetCommand.call()

            Unit
        }
}