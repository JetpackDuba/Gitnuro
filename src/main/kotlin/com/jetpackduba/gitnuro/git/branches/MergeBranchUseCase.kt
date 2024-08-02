package com.jetpackduba.gitnuro.git.branches

import com.jetpackduba.gitnuro.exceptions.ConflictsException
import com.jetpackduba.gitnuro.exceptions.UncommittedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class MergeBranchUseCase @Inject constructor() {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
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
            throw UncommittedChangesDetectedException("Merge failed, makes sure you repository doesn't contain uncommitted changes.")
        }

        mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING
    }
}