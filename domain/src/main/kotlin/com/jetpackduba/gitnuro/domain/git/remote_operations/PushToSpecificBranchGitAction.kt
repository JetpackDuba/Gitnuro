package com.jetpackduba.gitnuro.domain.git.remote_operations

import com.jetpackduba.gitnuro.domain.git.isRejected
import com.jetpackduba.gitnuro.domain.git.statusMessage
import com.jetpackduba.gitnuro.domain.remoteName
import com.jetpackduba.gitnuro.domain.simpleName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class PushToSpecificBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
) {
    suspend operator fun invoke(git: Git, force: Boolean, pushTags: Boolean, remoteBranch: Ref) =
        withContext(Dispatchers.IO) {
            val currentBranchRefSpec = git.repository.fullBranch

            handleTransportGitAction(git) {
                val pushResult = git
                    .push()
                    .setRefSpecs(RefSpec("$currentBranchRefSpec:${remoteBranch.simpleName}"))
                    .setRemote(remoteBranch.remoteName)
                    .setForce(force)
                    .apply {
                        if (pushTags)
                            setPushTags()
                    }
                    .setTransportConfigCallback { handleTransport(it) }
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
}