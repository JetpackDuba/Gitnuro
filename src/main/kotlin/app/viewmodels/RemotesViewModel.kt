package app.viewmodels

import app.git.BranchesManager
import app.git.RemoteInfo
import app.git.RemotesManager
import app.git.TabState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RemoteConfig
import javax.inject.Inject

class RemotesViewModel @Inject constructor(
    private val remotesManager: RemotesManager,
    private val branchesManager: BranchesManager,
) {
    private val _remotes = MutableStateFlow<List<RemoteInfo>>(listOf())
    val remotes: StateFlow<List<RemoteInfo>>
        get() = _remotes

    suspend fun loadRemotes(git: Git) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList()
            .call()
        val allRemoteBranches = branchesManager.remoteBranches(git)

        remotesManager.loadRemotes(git, allRemoteBranches)
        val remoteInfoList = remotes.map { remoteConfig ->
            val remoteBranches = allRemoteBranches.filter { branch ->
                branch.name.startsWith("refs/remotes/${remoteConfig.name}")
            }
            RemoteInfo(remoteConfig, remoteBranches)
        }

        _remotes.value = remoteInfoList
    }

    suspend fun refresh(git: Git) = withContext(Dispatchers.IO) {
        loadRemotes(git)
    }
}

