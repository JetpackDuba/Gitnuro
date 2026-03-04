package com.jetpackduba.gitnuro.domain.git.remote_operations

import com.jetpackduba.gitnuro.domain.remoteName
import com.jetpackduba.gitnuro.domain.simpleName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullFromSpecificBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val hasPullResultConflictsGitAction: HasPullResultConflictsGitAction,
) {
    suspend operator fun invoke(git: Git, remoteBranch: Ref, pullWithRebase: Boolean): PullHasConflicts =
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