package com.jetpackduba.gitnuro.git.rebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import java.io.File
import javax.inject.Inject

class GetRebaseAmendCommitIdUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): String? = withContext(Dispatchers.IO) {
        val repository = git.repository

        val amendFile = File(repository.directory, "${RebaseCommand.REBASE_MERGE}/${RebaseConstants.AMEND}")

        if (!amendFile.exists()) {
            return@withContext null
        }

        return@withContext amendFile.readText().removeSuffix("\n").removeSuffix("\r\n")
    }
}