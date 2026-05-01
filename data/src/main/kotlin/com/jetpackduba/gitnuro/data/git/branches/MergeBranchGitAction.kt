package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.domain.interfaces.IMergeBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.lib.ObjectId
import javax.inject.Inject


class MergeBranchGitAction @Inject constructor(
    private val jgit: JGit,
) : IMergeBranchGitAction {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
    override suspend operator fun invoke(
        repositoryPath: String,
        branch: Branch,
        fastForward: Boolean,
    ) = jgit.provide(repositoryPath) { git ->

        val fastForwardMode = if (fastForward)
            MergeCommand.FastForwardMode.FF
        else
            MergeCommand.FastForwardMode.NO_FF

        val mergeBase: ObjectId = git.repository.resolve(branch.name) ?: throw Exception("Branch ${branch.name} not found")

        val mergeResult = git
            .merge()
            .include(mergeBase)
            .setFastForward(fastForwardMode)
            .call()

        if (mergeResult.mergeStatus == MergeResult.MergeStatus.FAILED) {
            throw UncommittedChangesDetectedException("Merge failed, makes sure you repository doesn't contain uncommitted changes.")
        }

        val hasConflicts = mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING

        hasConflicts
    }
}