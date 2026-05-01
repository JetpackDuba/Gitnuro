package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IPopStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class PopStashGitAction @Inject constructor(
    private val applyStashGitAction: ApplyStashGitAction,
    private val deleteStashGitAction: DeleteStashGitAction,
    private val jgit: JGit,
) : IPopStashGitAction {
    override suspend operator fun invoke(repositoryPath: String, stash: Commit) = jgit.provide(repositoryPath) { git ->
        applyStashGitAction(git, stash)
        deleteStashGitAction(git.repository.directory.absolutePath, stash)

        Unit
    }
}