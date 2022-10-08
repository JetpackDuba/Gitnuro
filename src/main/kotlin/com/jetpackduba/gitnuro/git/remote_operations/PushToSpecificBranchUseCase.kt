package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.extensions.remoteName
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.isRejected
import com.jetpackduba.gitnuro.git.statusMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class PushToSpecificBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
) {
    suspend operator fun invoke(git: Git, force: Boolean, pushTags: Boolean, remoteBranch: Ref) =
        withContext(Dispatchers.IO) {
            val currentBranchRefSpec = git.repository.fullBranch

            val pushResult = git
                .push()
                .setRefSpecs(RefSpec("$currentBranchRefSpec:${remoteBranch.simpleName}"))
                .setRemote(remoteBranch.remoteName)
                .setForce(force)
                .apply {
                    if (pushTags)
                        setPushTags()
                }
                .setTransportConfigCallback { handleTransportUseCase(it, git) }
                .call()

            val results =
                pushResult.map { it.remoteUpdates.filter { remoteRefUpdate -> remoteRefUpdate.status.isRejected } }
                    .flatten()
            if (results.isNotEmpty()) {
                val error = StringBuilder()

                results.forEach { result ->
                    error.append(result.statusMessage)
                    error.append("\n")
                }

                throw Exception(error.toString())
            }
        }
}