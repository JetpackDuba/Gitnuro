package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RemoteConfig
import org.eclipse.jgit.transport.URIish
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

    suspend fun deleteRemote(git: Git, remoteName: String) = withContext(Dispatchers.IO) {
        git.remoteRemove()
            .setRemoteName(remoteName)
            .call()
    }

    suspend fun addRemote(git: Git, remoteName: String, fetchUri: String) = withContext(Dispatchers.IO) {
        git.remoteAdd()
            .setName(remoteName)
            .setUri(URIish(fetchUri))
            .call()
    }

    suspend fun updateRemote(git: Git, remoteName: String, uri: String, uriType: RemoteSetUrlCommand.UriType) =
        withContext(Dispatchers.IO) {
            git.remoteSetUrl()
                .setRemoteName(remoteName)
                .setRemoteUri(URIish(uri))
                .setUriType(uriType)
                .call()
        }
}

data class RemoteInfo(val remoteConfig: RemoteConfig, val branchesList: List<Ref>)