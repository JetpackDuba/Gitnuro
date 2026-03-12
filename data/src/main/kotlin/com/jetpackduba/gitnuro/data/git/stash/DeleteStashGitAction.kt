package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.domain.interfaces.IDeleteStashGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class DeleteStashGitAction @Inject constructor(
    private val getStashListGitAction: GetStashListGitAction,
) : IDeleteStashGitAction {
    override suspend operator fun invoke(git: Git, stashInfo: RevCommit): Unit = withContext(Dispatchers.IO) {
        val stashList = getStashListGitAction(git)
        val indexOfStashToDelete = stashList.indexOf(stashInfo)

        git.stashDrop()
            .setStashRef(indexOfStashToDelete)
            .call()
    }
}