package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IStageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import javax.inject.Inject

class StageEntryGitAction @Inject constructor(
    private val jgit: JGit,
) : IStageEntryGitAction {
    override suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry) =
        jgit.provide(repositoryPath) { git ->
            val files = if (statusEntry.statusType == StatusType.RENAMED) {
                listOf(statusEntry.oldPath, statusEntry.newPath)
            } else {
                listOf(statusEntry.filePath)
            }

            git
                .add()
                .addFilepatterns(files)
                .setUpdate(statusEntry.statusType == StatusType.REMOVED)
                .call()

            Unit
        }
}