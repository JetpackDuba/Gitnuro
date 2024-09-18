package com.jetpackduba.gitnuro.git.remote_operations

import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.RebaseResult
import javax.inject.Inject

typealias PullHasConflicts = Boolean

class HasPullResultConflictsUseCase @Inject constructor() {
    operator fun invoke(isRebase: Boolean, pullResult: PullResult): PullHasConflicts {
        if (!pullResult.isSuccessful) {
            if (
                pullResult.mergeResult?.mergeStatus == MergeResult.MergeStatus.CONFLICTING ||
                pullResult.rebaseResult?.status == RebaseResult.Status.CONFLICTS ||
                pullResult.rebaseResult?.status == RebaseResult.Status.STOPPED
            ) {
                return true
            }

            if (isRebase) {
                val message = when (pullResult.rebaseResult.status) {
                    RebaseResult.Status.UNCOMMITTED_CHANGES -> "The pull with rebase has failed because you have got uncommitted changes"
                    else -> "Pull failed"
                }

                throw Exception(message)
            }
        }

        return false
    }

}