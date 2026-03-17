package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.stash.DeleteStashGitAction
import com.jetpackduba.gitnuro.data.git.stash.SnapshotStashCreateCommand
import com.jetpackduba.gitnuro.data.git.workspace.CheckHasUncommittedChangesGitAction
import com.jetpackduba.gitnuro.domain.exceptions.GitnuroException
import com.jetpackduba.gitnuro.domain.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.domain.interfaces.IMergeBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject


class MergeBranchGitAction @Inject constructor(
    private val checkHasUncommittedChangesGitAction: CheckHasUncommittedChangesGitAction,
    private val deleteStashGitAction: DeleteStashGitAction,
) : IMergeBranchGitAction {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
    override suspend operator fun invoke(
        git: Git,
        branch: Branch,
        fastForward: Boolean,
        mergeAutoStash: Boolean,
    ) = withContext(Dispatchers.IO) {
        var backupStash: RevCommit? = null

        if (mergeAutoStash) {
            val hasUncommitedChanges = checkHasUncommittedChangesGitAction(git)
            if (hasUncommitedChanges) {
                val snapshotStashCreateCommand = SnapshotStashCreateCommand(
                    repository = git.repository,
                    // TODO Fix this
                    workingDirectoryMessage = "TMP MESSAGE"/*getString(
                        Res.string.merge_automatic_stash_description,
                        branch.simpleName,
                        git.repository.branch
                    )*/,
                    includeUntracked = true
                )

                backupStash = snapshotStashCreateCommand.call()
            }
        }

        val fastForwardMode = if (fastForward)
            MergeCommand.FastForwardMode.FF
        else
            MergeCommand.FastForwardMode.NO_FF

        val mergeBase: ObjectId =
            git.repository.resolve(branch.name) ?: throw Exception("Branch ${branch.name} not found")

        val mergeResult = git
            .merge()
            .include(mergeBase)
            .setFastForward(fastForwardMode)
            .call()

        if (mergeResult.mergeStatus == MergeResult.MergeStatus.FAILED) {
            throw UncommittedChangesDetectedException("Merge failed, makes sure you repository doesn't contain uncommitted changes.")
        }

        val hasConflicts = mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING

        if (!hasConflicts && backupStash != null) {
            deleteStashGitAction(git, backupStash)
        }

        return@withContext hasConflicts
    }
}