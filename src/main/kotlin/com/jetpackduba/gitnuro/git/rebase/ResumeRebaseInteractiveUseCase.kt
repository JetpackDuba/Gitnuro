package com.jetpackduba.gitnuro.git.rebase

import com.jetpackduba.gitnuro.exceptions.UncommittedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import javax.inject.Inject

class ResumeRebaseInteractiveUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, interactiveHandler: RebaseCommand.InteractiveHandler) =
        withContext(Dispatchers.IO) {
            val rebaseResult = git.rebase()
                .runInteractively(interactiveHandler)
                .setOperation(RebaseCommand.Operation.PROCESS_STEPS)
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