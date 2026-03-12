package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.domain.interfaces.IPopStashGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class PopStashGitAction @Inject constructor(
    private val applyStashGitAction: ApplyStashGitAction,
    private val deleteStashGitAction: DeleteStashGitAction,
) : IPopStashGitAction {
    override suspend operator fun invoke(git: Git, stash: RevCommit) = withContext(Dispatchers.IO) {
        applyStashGitAction(git, stash)
        deleteStashGitAction(git, stash)
    }
}