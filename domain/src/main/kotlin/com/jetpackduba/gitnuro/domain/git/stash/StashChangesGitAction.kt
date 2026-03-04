package com.jetpackduba.gitnuro.domain.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StashChangesGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git, message: String?): Boolean = withContext(Dispatchers.IO) {
        val commit = git
            .stashCreate()
            .setIncludeUntracked(true)
            .apply {
                if (message != null)
                    setWorkingDirectoryMessage(message)
            }
            .call()

        commit != null
    }
}