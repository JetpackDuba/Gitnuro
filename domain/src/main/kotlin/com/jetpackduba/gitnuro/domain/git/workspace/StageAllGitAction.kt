package com.jetpackduba.gitnuro.domain.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject


class StageAllGitAction @Inject constructor(
    private val getStatusGitAction: GetStatusGitAction,
) {
    suspend operator fun invoke(git: Git, entries: List<StatusEntry>?): Unit = withContext(Dispatchers.IO) {
        val status = getStatusGitAction(git.repository.directory.absolutePath)
        val unstaged = status.unstaged
            .run {
                if (entries != null) {
                    this.filter { entries.contains(it) }
                } else {
                    this
                }
            }


        addAllExceptNew(git, unstaged.filter { it.statusType != StatusType.ADDED })
        addNewFiles(git, unstaged.filter { it.statusType == StatusType.ADDED })
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