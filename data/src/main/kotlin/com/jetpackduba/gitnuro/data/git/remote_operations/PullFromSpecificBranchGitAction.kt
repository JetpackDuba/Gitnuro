package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IPullFromSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullFromSpecificBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val hasPullResultConflictsGitAction: HasPullResultConflictsGitAction,
) : IPullFromSpecificBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, remoteBranch: Branch, pullWithRebase: Boolean) =
        jgit(repositoryPath) {
            handleTransportGitAction(repositoryPath) {
                val pullResult = pull()
                    .setTransportConfigCallback { handleTransport(it) }
                    .setRemote(remoteBranch.remoteName)
                    .setRemoteBranchName(remoteBranch.simpleName)
                    .setRebase(pullWithRebase)
                    .setCredentialsProvider(CredentialsProvider.getDefault())
                    .call()

                return@handleTransportGitAction hasPullResultConflictsGitAction(pullWithRebase, pullResult)
            }
        }
}