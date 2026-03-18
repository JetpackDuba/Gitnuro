package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.domain.exceptions.RevertCommitException
import com.jetpackduba.gitnuro.domain.interfaces.IRevertCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import javax.inject.Inject

class RevertCommitGitAction @Inject constructor() : IRevertCommitGitAction {
    override suspend operator fun invoke(git: Git, revCommit: Commit): Unit = withContext(Dispatchers.IO) {
        val base =
            git.repository.resolve(revCommit.hash) ?: throw Exception("Commit ${revCommit.hash} not found")

        val revertCommand = git
            .revert()
            .include(base)

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