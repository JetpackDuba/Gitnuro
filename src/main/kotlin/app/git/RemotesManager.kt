package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RemoteConfig
import javax.inject.Inject

class RemotesManager @Inject constructor() {
    private val _remotes = MutableStateFlow<List<RemoteInfo>>(listOf())
    val remotes: StateFlow<List<RemoteInfo>>
        get() = _remotes

    suspend fun loadRemotes(git: Git, allRemoteBranches: List<Ref>) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList()
            .call()

        val remoteInfoList = remotes.map { remoteConfig ->
            val remoteBranches = allRemoteBranches.filter { branch ->
                branch.name.startsWith("refs/remotes/${remoteConfig.name}")
            }
            RemoteInfo(remoteConfig, remoteBranches)
        }

        _remotes.value = remoteInfoList
    }
}

data class RemoteInfo(val remoteConfig: RemoteConfig, val branchesList: List<Ref>)