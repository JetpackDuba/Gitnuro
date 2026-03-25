package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.errors.either
import com.jetpackduba.gitnuro.domain.interfaces.IStageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject


class StageAllGitAction @Inject constructor(
    private val getStatusGitAction: GetStatusGitAction,
) : IStageAllGitAction {
    override suspend operator fun invoke(repositoryPath: String, entries: List<StatusEntry>?) = either {
        val status = getStatusGitAction(repositoryPath).bind()

        jgit(repositoryPath) {
            val unstaged = status.unstaged
                .run {
                    if (entries != null) {
                        this.filter { entries.contains(it) }
                    } else {
                        this
                    }
                }


            addAllExceptNew(this, unstaged.filter { it.statusType != StatusType.ADDED })
            addNewFiles(this, unstaged.filter { it.statusType == StatusType.ADDED })
        }
    }

    /**
     * The setUpdate flag of the addCommand adds deleted files but not newly added when active
     */
    private fun addAllExceptNew(git: Git, allExceptNew: List<StatusEntry>) {
        if (allExceptNew.isEmpty())
            return

        val addCommand = git
            .add()

        for (entry in allExceptNew) {
            addCommand.addFilepattern(entry.filePath)
        }

        addCommand.setUpdate(true)

        addCommand.call()
    }

    private fun addNewFiles(git: Git, newFiles: List<StatusEntry>) {
        if (newFiles.isEmpty())
            return

        val addCommand = git
            .add()

        for (path in newFiles) {
            addCommand.addFilepattern(path.filePath)
        }

        addCommand.setUpdate(false)

        addCommand.call()
    }
}