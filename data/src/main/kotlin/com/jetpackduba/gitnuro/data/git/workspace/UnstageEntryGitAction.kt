package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import javax.inject.Inject

class UnstageEntryGitAction @Inject constructor(
    private val jgit: JGit,
) : IUnstageEntryGitAction {
    override suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry) =
        jgit.provide(repositoryPath) { git ->
            val files = if (statusEntry.statusType == StatusType.RENAMED) {
                listOf(statusEntry.oldPath, statusEntry.newPath)
            } else {
                listOf(statusEntry.filePath)
            }

            val resetCommand = git.reset()

            for (file in files) {
                resetCommand.addPath(file)
            }

            resetCommand.call()

            Unit
        }
}