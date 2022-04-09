package app.git

import app.exceptions.UncommitedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class RebaseManager @Inject constructor() {

    suspend fun rebaseBranch(git: Git, ref: Ref) = withContext(Dispatchers.IO) {
        val rebaseResult = git.rebase()
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(ref.objectId)
            .call()

        if(rebaseResult.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
            throw UncommitedChangesDetectedException("Rebase failed, the repository contains uncommited changes.")
        }
    }

    suspend fun continueRebase(git: Git) = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.CONTINUE)
            .call()
    }

    suspend fun abortRebase(git: Git) = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.ABORT)
            .call()
    }

    suspend fun skipRebase(git: Git) = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.SKIP)
            .call()
    }

    suspend fun rebaseInteractive(git: Git, interactiveHandler: InteractiveHandler, commit: RevCommit) {
        //TODO Check possible rebase errors by checking the result
        git.rebase()
            .runInteractively(interactiveHandler)
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(commit)
            .call()
    }
}