package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.domain.interfaces.IResumeRebaseInteractiveGitAction
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import javax.inject.Inject

class ResumeRebaseInteractiveGitAction @Inject constructor(
    private val jgit: JGit,
) : IResumeRebaseInteractiveGitAction {
    override suspend operator fun invoke(repositoryPath: String, interactiveHandler: RebaseCommand.InteractiveHandler) =
        jgit.provide(repositoryPath) { git ->
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