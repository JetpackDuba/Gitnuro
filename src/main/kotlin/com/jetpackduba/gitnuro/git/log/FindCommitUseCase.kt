package com.jetpackduba.gitnuro.git.log

import com.jetpackduba.gitnuro.logging.printError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val TAG = "FindCommitUseCase"

class FindCommitUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, commitId: String): RevCommit? = withContext(Dispatchers.IO) {
        val objectId = ObjectId.fromString(commitId)
        return@withContext invoke(git, objectId)
    }

    suspend operator fun invoke(git: Git, commitId: ObjectId): RevCommit? = withContext(Dispatchers.IO) {
        return@withContext try {
            git.repository.parseCommit(commitId)
        } catch (ex: Exception) {
            printError(TAG, "Commit $commitId not found", ex)
            null
        }
    }
}