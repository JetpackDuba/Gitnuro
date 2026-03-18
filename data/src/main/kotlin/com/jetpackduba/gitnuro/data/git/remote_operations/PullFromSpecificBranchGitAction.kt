package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.domain.interfaces.IPullFromSpecificBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.PullHasConflicts
import com.jetpackduba.gitnuro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullFromSpecificBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val hasPullResultConflictsGitAction: HasPullResultConflictsGitAction,
) : IPullFromSpecificBranchGitAction {
    override suspend operator fun invoke(git: Git, remoteBranch: Branch, pullWithRebase: Boolean): PullHasConflicts =
        withContext(Dispatchers.IO) {
            handleTransportGitAction(git) {
                val pullResult = git
                    .pull()
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