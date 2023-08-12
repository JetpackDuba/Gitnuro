package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.git.branches.DeleteBranchUseCase
import com.jetpackduba.gitnuro.git.isRejected
import com.jetpackduba.gitnuro.git.statusMessage
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class DeleteRemoteBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
) {
    suspend operator fun invoke(git: Git, ref: Ref) {
        val branchSplit = ref.name.split("/").toMutableList()
        val remoteName = branchSplit[2] // Remote name
        repeat(3) {
            branchSplit.removeAt(0)
        }

        val branchName = "refs/heads/${branchSplit.joinToString("/")}"

        val refSpec = RefSpec()
            .setSource(null)
            .setDestination(branchName)

        handleTransportUseCase(git) {
            val pushResults = git.push()
                .setTransportConfigCallback {
                    handleTransport(it)
                }
                .setRefSpecs(refSpec)
                .setRemote(remoteName)
                .call()

            val results = pushResults.map { pushResult ->
                pushResult.remoteUpdates.filter { remoteRefUpdate ->
                    remoteRefUpdate.status.isRejected
                }
            }.flatten()

            if (results.isNotEmpty()) {
                val error = StringBuilder()

                results.forEach { result ->
                    error.append(result.statusMessage)
                    error.append("\n")
                }

                throw Exception(error.toString())
            }
        }
        deleteBranchUseCase(git, ref)

    }
}