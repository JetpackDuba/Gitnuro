package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.exceptions.InvalidRemoteUrlException
import com.jetpackduba.gitnuro.extensions.BranchNameContainsFilter
import com.jetpackduba.gitnuro.extensions.OriginFilter
import com.jetpackduba.gitnuro.extensions.matchesAll
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.DeleteLocallyRemoteBranchesUseCase
import com.jetpackduba.gitnuro.git.branches.GetRemoteBranchesUseCase
import com.jetpackduba.gitnuro.git.remote_operations.DeleteRemoteBranchUseCase
import com.jetpackduba.gitnuro.git.remotes.*
import com.jetpackduba.gitnuro.ui.dialogs.RemoteWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RemotesViewModel @Inject constructor(
    private val tabState: TabState,
    private val deleteRemoteBranchUseCase: DeleteRemoteBranchUseCase,
    private val getRemoteBranchesUseCase: GetRemoteBranchesUseCase,
    private val getRemotesUseCase: GetRemotesUseCase,
    private val deleteRemoteUseCase: DeleteRemoteUseCase,
    private val addRemoteUseCase: AddRemoteUseCase,
    private val updateRemoteUseCase: UpdateRemoteUseCase,
    private val deleteLocallyRemoteBranchesUseCase: DeleteLocallyRemoteBranchesUseCase,
    private val tabScope: CoroutineScope,
) : ExpandableViewModel() {
    private val _remotes = MutableStateFlow<List<RemoteView>>(listOf())
    val remotes: StateFlow<List<RemoteView>>
        get() = _remotes

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.REMOTES, RefreshType.BRANCH_FILTER) {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadRemotes(git: Git) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList()
            .call()
        val allRemoteBranches = getRemoteBranchesUseCase(git)

        getRemotesUseCase(git, allRemoteBranches)

        val remoteInfoList = remotes.map { remoteConfig ->
            val filters = listOf(
                OriginFilter(remoteName = remoteConfig.name),
                BranchNameContainsFilter(keyword = tabState.branchFilterKeyword.value)
            )

            val remoteBranches = allRemoteBranches
                .filter { it.matchesAll(filters) }

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
        deleteRemoteBranchUseCase(git, ref)
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
        deleteRemoteUseCase(git, remoteName)

        val remoteBranches = getRemoteBranchesUseCase(git)
        val remoteToDeleteBranchesNames = remoteBranches.filter {
            it.name.startsWith("refs/remotes/$remoteName/")
        }.map {
            it.name
        }

        deleteLocallyRemoteBranchesUseCase(git, remoteToDeleteBranchesNames)
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

        addRemoteUseCase(git, selectedRemoteConfig.remoteName, selectedRemoteConfig.fetchUri)

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

        updateRemoteUseCase(
            git = git,
            remoteName = selectedRemoteConfig.remoteName,
            uri = selectedRemoteConfig.fetchUri,
            uriType = RemoteSetUrlCommand.UriType.FETCH
        )

        updateRemoteUseCase(
            git = git,
            remoteName = selectedRemoteConfig.remoteName,
            uri = selectedRemoteConfig.pushUri,
            uriType = RemoteSetUrlCommand.UriType.PUSH
        )
    }
}

data class RemoteView(val remoteInfo: RemoteInfo, val isExpanded: Boolean)