package com.jetpackduba.gitnuro.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class ResetEntryUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, statusEntry: StatusEntry, staged: Boolean): Unit =
        withContext(Dispatchers.IO) {
            if (staged || statusEntry.statusType == StatusType.CONFLICTING) {
                git
                    .reset()
                    .addPath(statusEntry.filePath)
                    .call()
            }

            git
                .checkout()
                .addPath(statusEntry.filePath)
                .call()
        }
}