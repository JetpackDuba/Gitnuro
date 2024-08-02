package com.jetpackduba.gitnuro.git.stash

import com.jetpackduba.gitnuro.models.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StashChangesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, message: String?): Success = withContext(Dispatchers.IO) {
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