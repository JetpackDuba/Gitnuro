package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.exceptions.RevertCommitException
import com.jetpackduba.gitnuro.domain.interfaces.IRevertCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.MergeResult
import javax.inject.Inject

class RevertCommitGitAction @Inject constructor(
    private val jgit: JGit,
) : IRevertCommitGitAction {
    override suspend operator fun invoke(repositoryPath: String, commit: Commit) = jgit.provide(repositoryPath) { git ->
        val base =
            git.repository.resolve(commit.hash) ?: throw Exception("Commit ${commit.hash} not found")

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