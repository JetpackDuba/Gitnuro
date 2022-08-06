package app.git

import app.exceptions.UncommitedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class MergeManager @Inject constructor() {
    suspend fun mergeBranch(git: Git, branch: Ref, fastForward: Boolean) = withContext(Dispatchers.IO) {
        val fastForwardMode = if (fastForward)
            MergeCommand.FastForwardMode.FF
        else
            MergeCommand.FastForwardMode.NO_FF

        val mergeResult = git
            .merge()
            .include(branch)
            .setFastForward(fastForwardMode)
            .call()

        if (mergeResult.mergeStatus == MergeResult.MergeStatus.FAILED) {
            throw UncommitedChangesDetectedException("Merge failed, makes sure you repository doesn't contain uncommited changes.")
        }
    }

    suspend fun resetRepoState(git: Git) = withContext(Dispatchers.IO) {
        git.repository.writeMergeCommitMsg(null)
        git.repository.writeMergeHeads(null)

        git.reset().setMode(ResetCommand.ResetType.HARD).call()
    }

    suspend fun cherryPickCommit(git: Git, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git.cherryPick()
            .include(revCommit)
            .call()
    }
}