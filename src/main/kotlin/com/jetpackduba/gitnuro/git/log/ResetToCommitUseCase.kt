package com.jetpackduba.gitnuro.git.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class ResetToCommitUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, revCommit: RevCommit, resetType: ResetType): Unit =
        withContext(Dispatchers.IO) {
            val reset = when (resetType) {
                ResetType.SOFT -> ResetCommand.ResetType.SOFT
                ResetType.MIXED -> ResetCommand.ResetType.MIXED
                ResetType.HARD -> ResetCommand.ResetType.HARD
            }
            git
                .reset()
                .setMode(reset)
                .setRef(revCommit.name)
                .call()
        }
}

enum class ResetType {
    SOFT,
    MIXED,
    HARD,
}