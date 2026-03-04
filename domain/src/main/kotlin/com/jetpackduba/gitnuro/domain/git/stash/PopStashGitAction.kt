package com.jetpackduba.gitnuro.domain.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class PopStashGitAction @Inject constructor(
    private val applyStashGitAction: ApplyStashGitAction,
    private val deleteStashGitAction: DeleteStashGitAction,
) {
    suspend operator fun invoke(git: Git, stash: RevCommit) = withContext(Dispatchers.IO) {
        applyStashGitAction(git, stash)
        deleteStashGitAction(git, stash)
    }
}