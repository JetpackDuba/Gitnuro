package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.interfaces.IStageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StageEntryGitAction @Inject constructor() : IStageEntryGitAction {
    override suspend operator fun invoke(git: Git, statusEntry: StatusEntry) = withContext(Dispatchers.IO) {
        git.add()
            .addFilepattern(statusEntry.filePath)
            .setUpdate(statusEntry.statusType == StatusType.REMOVED)
            .call()
    }
}