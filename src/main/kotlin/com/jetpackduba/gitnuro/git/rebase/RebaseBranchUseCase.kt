package com.jetpackduba.gitnuro.git.rebase

import com.jetpackduba.gitnuro.exceptions.UncommittedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

typealias IsMultiStep = Boolean

class RebaseBranchUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, ref: Ref): IsMultiStep = withContext(Dispatchers.IO) {
        val rebaseResult = git.rebase()
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(ref.objectId)
            .call()

        if (rebaseResult.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
            throw UncommittedChangesDetectedException("Rebase failed, the repository contains uncommitted changes.")
        }

        if (rebaseResult.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
            throw UncommittedChangesDetectedException("Merge failed, makes sure you repository doesn't contain uncommitted changes.")
        }

        return@withContext rebaseResult.status == RebaseResult.Status.STOPPED || rebaseResult.status == RebaseResult.Status.CONFLICTS
    }
}