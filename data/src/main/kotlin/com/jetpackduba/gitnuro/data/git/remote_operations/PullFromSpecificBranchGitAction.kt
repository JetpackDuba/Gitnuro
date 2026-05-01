package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IPullFromSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullFromSpecificBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val hasPullResultConflictsGitAction: HasPullResultConflictsGitAction,
    private val jgit: JGit,
) : IPullFromSpecificBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, remoteBranch: Branch, pullWithRebase: Boolean) =
        jgit.provide(repositoryPath) { git ->
            handleTransportGitAction(repositoryPath) {
                val pullResult = git
                    .pull()
                    .setTransportConfigCallback { handleTransport(it) }
                    .setRemote(remoteBranch.remoteName)
                    .setRemoteBranchName(remoteBranch.simpleName)
                    .setRebase(pullWithRebase)
                    .setCredentialsProvider(CredentialsProvider.getDefault())
                    .call()

                return@handleTransportGitAction hasPullResultConflictsGitAction(pullWithRebase, pullResult)
            }.bind()
        }
}