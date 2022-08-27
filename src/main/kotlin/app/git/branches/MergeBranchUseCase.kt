package app.git.branches

import app.exceptions.UncommitedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class MergeBranchUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, branch: Ref, fastForward: Boolean) = withContext(Dispatchers.IO) {
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
}