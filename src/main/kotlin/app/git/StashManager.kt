package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
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

    suspend fun getStashList(git: Git) = withContext(Dispatchers.IO) {
        return@withContext git
            .stashList()
            .call()
    }
}