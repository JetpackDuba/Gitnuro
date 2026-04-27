package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.data.git.stash.DeleteStashGitAction
import com.jetpackduba.gitnuro.data.git.stash.SnapshotStashCreateCommand
import com.jetpackduba.gitnuro.data.git.workspace.CheckHasUncommittedChangesGitAction
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.exceptions.GitnuroException
import com.jetpackduba.gitnuro.domain.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.domain.interfaces.IMergeBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject


class MergeBranchGitAction @Inject constructor() : IMergeBranchGitAction {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
    override suspend operator fun invoke(
        repositoryPath: String,
        branch: Branch,
        fastForward: Boolean,
    ) = jgit(repositoryPath) {

        val fastForwardMode = if (fastForward)
            MergeCommand.FastForwardMode.FF
        else
            MergeCommand.FastForwardMode.NO_FF

        val mergeBase: ObjectId =
            this.repository.resolve(branch.name) ?: throw Exception("Branch ${branch.name} not found")

        val mergeResult = this
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