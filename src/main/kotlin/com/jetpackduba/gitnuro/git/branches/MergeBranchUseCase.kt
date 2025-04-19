package com.jetpackduba.gitnuro.git.branches

import com.jetpackduba.gitnuro.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.merge_automatic_stash_description
import com.jetpackduba.gitnuro.git.stash.DeleteStashUseCase
import com.jetpackduba.gitnuro.git.stash.SnapshotStashCreateCommand
import com.jetpackduba.gitnuro.git.workspace.CheckHasUncommittedChangesUseCase
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.resources.getString
import javax.inject.Inject

class MergeBranchUseCase @Inject constructor(
    private val checkHasUncommittedChangesUseCase: CheckHasUncommittedChangesUseCase,
    private val deleteStashUseCase: DeleteStashUseCase,
    private val appSettingsRepository: AppSettingsRepository,
) {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
    suspend operator fun invoke(git: Git, branch: Ref, fastForward: Boolean) = withContext(Dispatchers.IO) {
        var backupStash: RevCommit? = null

        if (appSettingsRepository.mergeAutoStash) {
            val hasUncommitedChanges = checkHasUncommittedChangesUseCase(git)
            if (hasUncommitedChanges) {
                val snapshotStashCreateCommand = SnapshotStashCreateCommand(
                    repository = git.repository,
                    workingDirectoryMessage = getString(
                        Res.string.merge_automatic_stash_description,
                        branch.simpleName,
                        git.repository.branch
                    ),
                    includeUntracked = true
                )

                backupStash = snapshotStashCreateCommand.call()
            }
        }

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

        val hasConflicts = mergeResult.mergeStatus == MergeResult.MergeStatus.CONFLICTING

        if (!hasConflicts && backupStash != null) {
            deleteStashUseCase(git, backupStash)
        }

        return@withContext hasConflicts
    }
}