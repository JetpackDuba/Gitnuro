package app.git.rebase

import app.exceptions.UncommitedChangesDetectedException
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

            if (rebaseResult.status == RebaseResult.Status.FAILED) {
                throw UncommitedChangesDetectedException("Rebase interactive failed.")
            }
        }
}