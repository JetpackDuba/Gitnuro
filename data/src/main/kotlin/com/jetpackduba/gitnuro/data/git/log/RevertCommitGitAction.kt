package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.domain.exceptions.RevertCommitException
import com.jetpackduba.gitnuro.domain.interfaces.IRevertCommitGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class RevertCommitGitAction @Inject constructor() : IRevertCommitGitAction {
    override suspend operator fun invoke(git: Git, revCommit: RevCommit): Unit = withContext(Dispatchers.IO) {
        val revertCommand = git
            .revert()
            .include(revCommit)

        revertCommand.call()

        val failingResult: MergeResult? = revertCommand.failingResult

        when (failingResult?.mergeStatus) {
            MergeResult.MergeStatus.FAILED -> throw RevertCommitException("Revert failed. Clear your workspace from uncommitted changes.")
            MergeResult.MergeStatus.CONFLICTING -> throw RevertCommitException("Revert failed. Fix the conflicts and commit the desired changes.")
            MergeResult.MergeStatus.ABORTED -> throw RevertCommitException("Revert aborted.")
            else -> {}
        }
    }
}