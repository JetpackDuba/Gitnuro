package com.jetpackduba.gitnuro.git.rebase

import com.jetpackduba.gitnuro.exceptions.UncommitedChangesDetectedException
import com.jetpackduba.gitnuro.logging.printDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import javax.inject.Inject

private const val GIT_REBASE_TODO = "git-rebase-todo"
private const val TAG = "StartRebaseInteractiveU"

class StartRebaseInteractiveUseCase @Inject constructor() {
    suspend operator fun invoke(
        git: Git,
        interactiveHandler: RebaseCommand.InteractiveHandler,
        commit: RevCommit,
        stop: Boolean
    ): List<RebaseTodoLine> =
        withContext(Dispatchers.IO) {
            val rebaseResult = git.rebase()
                .runInteractively(interactiveHandler, stop)
                .setOperation(RebaseCommand.Operation.BEGIN)
                .setUpstream(commit)
                .call()

            when (rebaseResult.status) {
                RebaseResult.Status.FAILED -> throw UncommitedChangesDetectedException("Rebase interactive failed.")
                RebaseResult.Status.UNCOMMITTED_CHANGES, RebaseResult.Status.CONFLICTS -> throw UncommitedChangesDetectedException(
                    "You can't have uncommited changes before starting a rebase interactive"
                )

                else -> {}
            }

            val repository = git.repository
            val lines = repository.readRebaseTodo("${RebaseCommand.REBASE_MERGE}/$GIT_REBASE_TODO", false)

            printDebug(TAG, "There are ${lines.count()} lines")

            return@withContext lines
        }
}