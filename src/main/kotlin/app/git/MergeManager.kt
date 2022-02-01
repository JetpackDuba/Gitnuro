package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class MergeManager @Inject constructor() {
    suspend fun mergeBranch(git: Git, branch: Ref, fastForward: Boolean) = withContext(Dispatchers.IO) {
        val fastForwardMode = if (fastForward)
            MergeCommand.FastForwardMode.FF
        else
            MergeCommand.FastForwardMode.NO_FF

        git
            .merge()
            .include(branch)
            .setFastForward(fastForwardMode)
            .call()
    }

    suspend fun abortMerge(git: Git) = withContext(Dispatchers.IO) {
        git.repository.writeMergeCommitMsg(null)
        git.repository.writeMergeHeads(null)

        git.reset().setMode(ResetCommand.ResetType.HARD).call()
    }
}