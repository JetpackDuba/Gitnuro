package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseAmendCommitIdGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import java.io.File
import javax.inject.Inject

class GetRebaseAmendCommitIdGitAction @Inject constructor() : IGetRebaseAmendCommitIdGitAction {
    override suspend operator fun invoke(git: Git): String? = withContext(Dispatchers.IO) {
        val repository = git.repository

        val amendFile = File(repository.directory, "${RebaseCommand.REBASE_MERGE}/${RebaseConstants.AMEND}")

        if (!amendFile.exists()) {
            return@withContext null
        }

        return@withContext amendFile.readText().removeSuffix("\n").removeSuffix("\r\n")
    }
}