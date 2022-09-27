package com.jetpackduba.gitnuro.git.rebase

import com.jetpackduba.gitnuro.exceptions.UncommitedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
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
            throw UncommitedChangesDetectedException("Rebase failed, the repository contains uncommited changes.")
        }
    }
}