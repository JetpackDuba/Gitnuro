package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RemoteConfig
import javax.inject.Inject

class RemotesManager @Inject constructor() {


    suspend fun loadRemotes(git: Git, allRemoteBranches: List<Ref>) = withContext(Dispatchers.IO) {
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

data class RemoteInfo(val remoteConfig: RemoteConfig, val branchesList: List<Ref>)