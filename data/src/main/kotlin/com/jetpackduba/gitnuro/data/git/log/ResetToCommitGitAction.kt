package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.domain.interfaces.IResetToCommitGitAction
import com.jetpackduba.gitnuro.domain.usecases.ResetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class ResetToCommitGitAction @Inject constructor() : IResetToCommitGitAction {
    override suspend operator fun invoke(git: Git, revCommit: RevCommit, resetType: ResetType): Unit =
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
