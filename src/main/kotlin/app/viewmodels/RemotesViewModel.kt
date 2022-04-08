package app.viewmodels

import app.exceptions.InvalidRemoteUrlException
import app.git.*
import app.ui.dialogs.RemoteWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RemotesViewModel @Inject constructor(
    private val remotesManager: RemotesManager,
    private val remoteOperationsManager: RemoteOperationsManager,
    private val branchesManager: BranchesManager,
    private val tabState: TabState,
) : ExpandableViewModel() {
    private val _remotes = MutableStateFlow<List<RemoteView>>(listOf())
    val remotes: StateFlow<List<RemoteView>>
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

        val remoteViewList = remoteInfoList.map { remoteInfo ->
            RemoteView(remoteInfo, true)
        }

        _remotes.value = remoteViewList
    }

    fun deleteRemoteBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        remoteOperationsManager.deleteBranch(git, ref)
    }

    suspend fun refresh(git: Git) = withContext(Dispatchers.IO) {
        loadRemotes(git)
    }

    fun onRemoteClicked(remoteInfo: RemoteView) {
        val remotes = _remotes.value
        val newRemoteInfo = remoteInfo.copy(isExpanded = !remoteInfo.isExpanded)
        val newRemotesList = remotes.toMutableList()
        val indexToReplace = newRemotesList.indexOf(remoteInfo)
        newRemotesList[indexToReplace] = newRemoteInfo

        _remotes.value = newRemotesList
    }

    fun selectBranch(ref: Ref) {
        tabState.newSelectedRef(ref.objectId)
    }

    fun deleteRemote(remoteName: String, isNew: Boolean) = tabState.safeProcessing(
        refreshType = if (isNew) RefreshType.REMOTES else RefreshType.ALL_DATA,
        showError = true,
    ) { git ->
        remotesManager.deleteRemote(git, remoteName)

        val remoteBranches = branchesManager.remoteBranches(git)
        val remoteToDeleteBranchesNames = remoteBranches.filter {
            it.name.startsWith("refs/remotes/$remoteName/")
        }.map {
            it.name
        }

        branchesManager.deleteLocallyRemoteBranches(git, remoteToDeleteBranchesNames)
    }


    fun addRemote(selectedRemoteConfig: RemoteWrapper) = tabState.runOperation(
        refreshType = RefreshType.REMOTES,
        showError = true,
    ) { git ->
        if (selectedRemoteConfig.fetchUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty fetch URI")
        }

        if (selectedRemoteConfig.pushUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty push URI")
        }

        remotesManager.addRemote(git, selectedRemoteConfig.remoteName, selectedRemoteConfig.fetchUri)

        updateRemote(selectedRemoteConfig) // Sets both, fetch and push uri
    }

    fun updateRemote(selectedRemoteConfig: RemoteWrapper) = tabState.runOperation(
        refreshType = RefreshType.REMOTES,
        showError = true,
    ) { git ->

        if (selectedRemoteConfig.fetchUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty fetch URI")
        }

        if (selectedRemoteConfig.pushUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty push URI")
        }

        remotesManager.updateRemote(
            git = git,
            remoteName = selectedRemoteConfig.remoteName,
            uri = selectedRemoteConfig.fetchUri,
            uriType = RemoteSetUrlCommand.UriType.FETCH
        )

        remotesManager.updateRemote(
            git = git,
            remoteName = selectedRemoteConfig.remoteName,
            uri = selectedRemoteConfig.pushUri,
            uriType = RemoteSetUrlCommand.UriType.PUSH
        )
    }
}

data class RemoteView(val remoteInfo: RemoteInfo, val isExpanded: Boolean)