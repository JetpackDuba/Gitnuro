package com.jetpackduba.gitnuro.git.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import javax.inject.Inject

class ResetRepositoryStateUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git.repository.apply {
            writeMergeCommitMsg(null)
            writeMergeHeads(null)
        }

        git.reset()
            .setMode(ResetCommand.ResetType.HARD)
            .call()
    }
}