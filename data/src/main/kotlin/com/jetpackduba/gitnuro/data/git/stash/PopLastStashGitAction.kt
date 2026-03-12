package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.domain.interfaces.IPopLastStashGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class PopLastStashGitAction @Inject constructor() : IPopLastStashGitAction {
    override suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git
            .stashApply()
            .call()

        git.stashDrop()
            .call()
    }
}