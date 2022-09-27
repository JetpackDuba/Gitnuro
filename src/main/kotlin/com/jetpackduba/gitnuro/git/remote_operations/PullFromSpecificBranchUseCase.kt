package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.extensions.remoteName
import com.jetpackduba.gitnuro.extensions.simpleName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullFromSpecificBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
) {
    suspend operator fun invoke(git: Git, rebase: Boolean, remoteBranch: Ref) = withContext(Dispatchers.IO) {
        val pullResult = git
            .pull()
            .setTransportConfigCallback { handleTransportUseCase(it) }
            .setRemote(remoteBranch.remoteName)
            .setRemoteBranchName(remoteBranch.simpleName)
            .setRebase(rebase)
            .setCredentialsProvider(CredentialsProvider.getDefault())
            .call()

        if (!pullResult.isSuccessful) {
            var message = "Pull failed" // TODO Remove messages from here and pass the result to a custom exception type

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