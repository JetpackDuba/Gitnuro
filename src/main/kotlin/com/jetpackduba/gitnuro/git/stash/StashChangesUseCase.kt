package com.jetpackduba.gitnuro.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StashChangesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, message: String?): Unit = withContext(Dispatchers.IO) {
        val commit = git
            .stashCreate()
            .setIncludeUntracked(true)
            .apply {
                if (message != null)
                    setWorkingDirectoryMessage(message)
            }
            .call()

        if (commit == null) {
            throw Exception("No changes to stash")
        }
    }
}