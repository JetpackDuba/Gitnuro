package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.domain.interfaces.IGetRemotesGitAction
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetRemotesGitAction @Inject constructor() : IGetRemotesGitAction {
    override suspend operator fun invoke(git: Git, allRemoteBranches: List<Ref>): List<RemoteInfo> =
        withContext(Dispatchers.IO) {
            val remotes = git.remoteList()
                .call()

            return@withContext remotes.map { remoteConfig ->
                val remoteBranches = allRemoteBranches.filter { branch ->
                    branch.name.startsWith("refs/remotes/${remoteConfig.name}")
                }
                RemoteInfo(remoteConfig, remoteBranches)
            }
        }
}