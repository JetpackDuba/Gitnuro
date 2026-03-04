package com.jetpackduba.gitnuro.domain.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class DeleteStashGitAction @Inject constructor(
    private val getStashListGitAction: GetStashListGitAction,
) {
    suspend operator fun invoke(git: Git, stashInfo: RevCommit): Unit = withContext(Dispatchers.IO) {
        val stashList = getStashListGitAction(git)
        val indexOfStashToDelete = stashList.indexOf(stashInfo)

        git.stashDrop()
            .setStashRef(indexOfStashToDelete)
            .call()
    }
}