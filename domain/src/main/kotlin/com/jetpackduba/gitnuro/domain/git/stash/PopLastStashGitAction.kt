package com.jetpackduba.gitnuro.domain.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class PopLastStashGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git
            .stashApply()
            .call()

        git.stashDrop()
            .call()
    }
}