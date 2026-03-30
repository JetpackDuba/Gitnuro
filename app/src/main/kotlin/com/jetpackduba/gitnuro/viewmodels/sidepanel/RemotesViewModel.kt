package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.extensions.toMutableSetAndAdd
import com.jetpackduba.gitnuro.domain.extensions.toMutableSetAndRemove
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.DeleteRemoteInfoUseCase
import com.jetpackduba.gitnuro.domain.usecases.SetClipboardContentUseCase
import com.jetpackduba.gitnuro.viewmodels.ISharedBranchesViewModel
import com.jetpackduba.gitnuro.viewmodels.ISharedRemotesViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedBranchesViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedRemotesViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git

class RemotesViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val fetchAllRemotesGitAction: IFetchAllRemotesGitAction,
    private val sharedBranchesViewModel: SharedBranchesViewModel,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val setClipboardContentUseCase: SetClipboardContentUseCase,
    private val deleteRemoteInfoUseCase: DeleteRemoteInfoUseCase,
    tabScope: TabCoroutineScope,
    sharedRemotesViewModel: SharedRemotesViewModel,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(false), ISharedRemotesViewModel by sharedRemotesViewModel,
    ISharedBranchesViewModel by sharedBranchesViewModel {

    private val remotes = repositoryDataRepository.remotes
    private val currentBranch = repositoryDataRepository.currentBranch
    private val remotesContracted = MutableStateFlow<Set<Remote>>(emptySet())


    val remoteState: StateFlow<RemotesState> =
        combine(
            remotes,
            isExpanded,
            filter,
            currentBranch,
            remotesContracted,
        ) { remotes, isExpanded, filter, currentBranch, remotesContracted ->
            val remotesFiltered = remotes.map { remoteInfo ->
                val newRemoteInfo = remoteInfo.copy(
                    branchesList = remoteInfo.branchesList.filter { branch ->
                        branch.simpleName.lowercaseContains(filter)
                    }
                )

                RemoteView(newRemoteInfo, isExpanded = !remotesContracted.contains(newRemoteInfo.remote))
            }

            RemotesState(
                remotesFiltered,
                isExpanded,
                currentBranch
            )
        }.stateIn(
            scope = tabScope,
            started = SharingStarted.Eagerly,
            initialValue = RemotesState(emptyList(), isExpanded.value, null)
        )

    fun onRemoteClicked(remoteClicked: RemoteView) {
        remotesContracted.value = if (remotesContracted.value.contains(remoteClicked.remoteInfo.remote)) {
            remotesContracted.value.toMutableSetAndRemove(remoteClicked.remoteInfo.remote)
        } else {
            remotesContracted.value.toMutableSetAndAdd(remoteClicked.remoteInfo.remote)
        }
    }

    fun selectBranch(ref: Branch) {
        tabState.newSelectedRef(ref, ref.hash)
    }

    fun deleteRemote(remoteInfo: RemoteInfo) = deleteRemoteInfoUseCase(remoteInfo)

    fun onFetchRemoteBranches(remote: RemoteView) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.FETCH,
    ) { git ->
        val remoteConfig = remote.remoteInfo.remote
        fetchAllRemotesGitAction(git, remoteConfig)

        positiveNotification("Fetched branches from ${remoteConfig.name}")
    }

    override fun copyBranchNameToClipboard(branch: Branch) = viewModelScope.launch {
        setClipboardContentUseCase(branch.simpleName, MessageType.BranchCopied(branch.simpleName))
    }
}

data class RemoteView(val remoteInfo: RemoteInfo, val isExpanded: Boolean)

data class RemotesState(val remotes: List<RemoteView>, val isExpanded: Boolean, val currentBranch: Branch?)