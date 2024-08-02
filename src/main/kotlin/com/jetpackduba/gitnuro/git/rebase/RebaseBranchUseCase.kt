package com.jetpackduba.gitnuro.git.rebase

import com.jetpackduba.gitnuro.exceptions.ConflictsException
import com.jetpackduba.gitnuro.exceptions.UncommittedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RebaseBranchUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, ref: Ref) = withContext(Dispatchers.IO) {
        val rebaseResult = git.rebase()
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(ref.objectId)
            .call()

        if (rebaseResult.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
            throw UncommittedChangesDetectedException("Rebase failed, the repository contains uncommitted changes.")
        }

        when (rebaseResult.status) {
            RebaseResult.Status.UNCOMMITTED_CHANGES -> throw UncommittedChangesDetectedException("Merge failed, makes sure you repository doesn't contain uncommitted changes.")
            RebaseResult.Status.CONFLICTS -> throw ConflictsException("Rebase produced conflicts, please fix them to continue.")
            else -> {}
        }
    }
}