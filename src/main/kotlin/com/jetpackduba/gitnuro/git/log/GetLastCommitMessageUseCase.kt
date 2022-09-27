package com.jetpackduba.gitnuro.git.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetLastCommitMessageUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): String = withContext(Dispatchers.IO) {
        try {
            val log = git.log().setMaxCount(1).call()
            val latestCommitNode = log.firstOrNull()

            return@withContext if (latestCommitNode == null)
                ""
            else
                latestCommitNode.fullMessage

        } catch (ex: Exception) {
            ex.printStackTrace()
            return@withContext ""
        }
    }
}