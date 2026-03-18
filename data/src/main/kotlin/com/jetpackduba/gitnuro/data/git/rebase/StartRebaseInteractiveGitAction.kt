package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.domain.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.domain.interfaces.IStartRebaseInteractiveGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject


class StartRebaseInteractiveGitAction @Inject constructor() : IStartRebaseInteractiveGitAction {
    override suspend operator fun invoke(
        git: Git,
        commit: Commit,
    ) = withContext(Dispatchers.IO) {
        val base = git.repository
            .resolve(commit.hash) ?: throw Exception("Commit ${commit.hash} not found")

        val interactiveHandler = object : RebaseCommand.InteractiveHandler {
            override fun prepareSteps(steps: MutableList<RebaseTodoLine>?) {}
            override fun modifyCommitMessage(message: String?): String = ""
        }

        val rebaseResult = git.rebase()
            .runInteractively(interactiveHandler, true)
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(base)
            .call()

        when (rebaseResult.status) {
            RebaseResult.Status.FAILED -> throw UncommittedChangesDetectedException("Rebase interactive failed.")
            RebaseResult.Status.UNCOMMITTED_CHANGES, RebaseResult.Status.CONFLICTS -> throw UncommittedChangesDetectedException(
                "You can't have uncommitted changes before starting a rebase interactive"
            )

            else -> {}
        }
    }
}