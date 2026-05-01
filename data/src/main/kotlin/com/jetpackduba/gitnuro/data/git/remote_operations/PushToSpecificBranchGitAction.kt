package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IPushToSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.isRejected
import com.jetpackduba.gitnuro.domain.models.statusMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class PushToSpecificBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val jgit: JGit,
) : IPushToSpecificBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, force: Boolean, pushTags: Boolean, remoteBranch: Branch) =
        jgit.provide(repositoryPath) {
            val currentBranchRefSpec = repository.fullBranch

            handleTransportGitAction(repositoryPath) {
                val pushResult = push()
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
                    pushResult.flatMap { it.remoteUpdates.filter { remoteRefUpdate -> remoteRefUpdate.status.isRejected } }
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