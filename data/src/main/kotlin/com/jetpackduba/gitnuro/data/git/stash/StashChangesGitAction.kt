package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.git.jgit2
import com.jetpackduba.gitnuro.domain.errors.StashChangesError
import com.jetpackduba.gitnuro.domain.errors.raiseError
import com.jetpackduba.gitnuro.domain.interfaces.IStashChangesGitAction
import javax.inject.Inject

class StashChangesGitAction @Inject constructor() : IStashChangesGitAction {
    override suspend operator fun invoke(repositoryPath: String, message: String?) = jgit2(repositoryPath) { git ->
        val commit = git
            .stashCreate()
            .setIncludeUntracked(true)
            .apply {
                if (message != null)
                    setWorkingDirectoryMessage(message)
            }
            .call()


        if (commit == null) {
            raiseError(StashChangesError.NoDataToStash)
        }
    }
}