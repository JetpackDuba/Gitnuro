package com.jetpackduba.gitnuro.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StageEntryUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, statusEntry: StatusEntry) = withContext(Dispatchers.IO) {
        git.add()
            .addFilepattern(statusEntry.filePath)
            .setUpdate(statusEntry.statusType == StatusType.REMOVED)
            .call()
    }
}