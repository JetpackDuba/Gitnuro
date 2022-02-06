package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class StashManager @Inject constructor() {
    suspend fun stash(git: Git) = withContext(Dispatchers.IO) {
        git
            .stashCreate()
            .setIncludeUntracked(true)
            .call()
    }

    suspend fun popStash(git: Git) = withContext(Dispatchers.IO) {
        git
            .stashApply()
            .call()

        git.stashDrop()
            .call()
    }

    suspend fun popStash(git: Git, stash: RevCommit) = withContext(Dispatchers.IO) {
        applyStash(git, stash)
        deleteStash(git, stash)
    }

    suspend fun getStashList(git: Git) = withContext(Dispatchers.IO) {
        return@withContext git
            .stashList()
            .call()
    }

    suspend fun applyStash(git: Git, stashInfo: RevCommit) = withContext(Dispatchers.IO) {
        git.stashApply()
            .setStashRef(stashInfo.name)
            .call()
    }

    suspend fun deleteStash(git: Git, stashInfo: RevCommit) = withContext(Dispatchers.IO) {
        val stashList = getStashList(git)
        val indexOfStashToDelete = stashList.indexOf(stashInfo)

        git.stashDrop()
            .setStashRef(indexOfStashToDelete)
            .call()
    }
}