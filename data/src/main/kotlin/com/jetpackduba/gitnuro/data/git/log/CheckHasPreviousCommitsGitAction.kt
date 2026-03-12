package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.domain.interfaces.ICheckHasPreviousCommitsGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class CheckHasPreviousCommitsGitAction @Inject constructor() : ICheckHasPreviousCommitsGitAction {
    override suspend operator fun invoke(git: Git): Boolean = withContext(Dispatchers.IO) {
        try {
            val log = git.log().setMaxCount(1).call()
            val latestCommitNode = log.firstOrNull()

            return@withContext latestCommitNode != null

        } catch (ex: Exception) {
            ex.printStackTrace()
            return@withContext false
        }
    }
}