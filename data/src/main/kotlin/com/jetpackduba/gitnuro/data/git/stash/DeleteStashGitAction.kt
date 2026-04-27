package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class DeleteStashGitAction @Inject constructor() : IDeleteStashGitAction {
    override suspend operator fun invoke(repositoryPath: String, stashInfo: Commit) = jgit(repositoryPath) {
        val stashList = stashList()
            .call()
            .toList()

        val indexOfStashToDelete = stashList.indexOfFirst { it.name == stashInfo.hash }

        if (indexOfStashToDelete != -1) {
            stashDrop()
                .setStashRef(indexOfStashToDelete)
                .call()
        }
    }
}