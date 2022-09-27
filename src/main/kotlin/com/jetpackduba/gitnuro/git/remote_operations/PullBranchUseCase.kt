package com.jetpackduba.gitnuro.git.remote_operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
) {
    suspend operator fun invoke(git: Git, rebase: Boolean) = withContext(Dispatchers.IO) {
        val pullResult = git
            .pull()
            .setTransportConfigCallback { handleTransportUseCase(it) }
            .setRebase(rebase)
            .setCredentialsProvider(CredentialsProvider.getDefault())
            .call()

        if (!pullResult.isSuccessful) {
            var message = "Pull failed"

            if (rebase) {
                message = when (pullResult.rebaseResult.status) {
                    RebaseResult.Status.UNCOMMITTED_CHANGES -> "The pull with rebase has failed because you have got uncommited changes"
                    RebaseResult.Status.CONFLICTS -> "Pull with rebase has conflicts, fix them to continue"
                    else -> message
                }
            }

            throw Exception(message)
        }
    }
}