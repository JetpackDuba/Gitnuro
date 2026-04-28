package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IApplyStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class ApplyStashGitAction @Inject constructor() : IApplyStashGitAction {
    override suspend operator fun invoke(repositoryPath: String, stashInfo: Commit) = jgit(repositoryPath) { git ->
        invoke(git, stashInfo)
    }

    operator fun invoke(git: Git, stashInfo: Commit) {
        git.stashApply()
            .setStashRef(stashInfo.hash)
            .call()
    }
}