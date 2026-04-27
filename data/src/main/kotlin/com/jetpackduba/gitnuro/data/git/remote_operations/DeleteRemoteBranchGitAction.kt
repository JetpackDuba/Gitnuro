package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.data.git.branches.DeleteBranchGitAction
import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.isRejected
import com.jetpackduba.gitnuro.domain.models.statusMessage
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class DeleteRemoteBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val deleteBranchGitAction: DeleteBranchGitAction,
) : IDeleteRemoteBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, ref: Branch) = jgit(repositoryPath) {
        val branchSplit = ref.name.split("/").toMutableList()
        val remoteName = branchSplit[2] // Remote name
        repeat(3) {
            branchSplit.removeAt(0)
        }

        val branchName = "refs/heads/${branchSplit.joinToString("/")}"

        val refSpec = RefSpec()
            .setSource(null)
            .setDestination(branchName)

        handleTransportGitAction(repositoryPath) {
            val pushResults = push()
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

        deleteBranchGitAction(repositoryPath, ref) /// TODO Handle error?

        Unit
    }
}